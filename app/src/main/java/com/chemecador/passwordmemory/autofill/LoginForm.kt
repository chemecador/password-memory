package com.chemecador.passwordmemory.autofill

import android.app.assist.AssistStructure
import android.app.assist.AssistStructure.ViewNode
import android.text.InputType
import android.view.View
import android.view.autofill.AutofillId

/**
 * The login form the system asked us about: which fields can be filled, what is already typed in
 * them, and where the form lives ([packageName] / [webDomain]), which is how entries are matched.
 */
data class LoginForm(
    val packageName: String?,
    val webDomain: String?,
    val usernameId: AutofillId?,
    val passwordIds: List<AutofillId>,
    val username: String?,
    val password: String?
) {

    /** Without a password field there is nothing to fill and nothing worth saving. */
    val isLogin: Boolean get() = passwordIds.isNotEmpty()

    /** The values typed by the user, once they submitted the form. */
    val credential: Pair<String?, String>?
        get() = password?.let { username to it }
}

/**
 * Finds the login form inside an [AssistStructure].
 *
 * Fields are recognised by their autofill hints first, since that is what a well-behaved app
 * provides, and by their input type, resource id, hint text and HTML attributes otherwise — which
 * is the common case in browsers and in apps that never got around to declaring hints. A text field
 * sitting right above a password field is taken as the username when nothing else identifies it.
 */
object LoginFormParser {

    fun parse(structure: AssistStructure): LoginForm {
        val scan = Scan()
        for (index in 0 until structure.windowNodeCount) {
            scan.visit(structure.getWindowNodeAt(index).rootViewNode)
        }
        return LoginForm(
            packageName = structure.activityComponent?.packageName,
            webDomain = scan.webDomain,
            usernameId = scan.username?.id,
            passwordIds = scan.passwords.map(Field::id),
            username = scan.username?.value,
            password = scan.passwords.firstNotNullOfOrNull(Field::value)
        )
    }

    /** Merges the forms of every context of a request: a two-step login splits them over two. */
    fun merge(forms: List<LoginForm>): LoginForm? {
        if (forms.isEmpty()) return null
        val newest = forms.asReversed()
        return LoginForm(
            packageName = newest.firstNotNullOfOrNull { it.packageName },
            webDomain = newest.firstNotNullOfOrNull { it.webDomain },
            usernameId = newest.firstNotNullOfOrNull { it.usernameId },
            passwordIds = forms.flatMap { it.passwordIds }.distinct(),
            username = newest.firstNotNullOfOrNull { it.username },
            password = newest.firstNotNullOfOrNull { it.password }
        )
    }
}

private data class Field(val id: AutofillId, val value: String?)

private enum class Kind { USERNAME, PASSWORD, OTHER }

private class Scan {

    var webDomain: String? = null
        private set
    var username: Field? = null
        private set
    val passwords = mutableListOf<Field>()

    /** Once a field says outright that it is the username, no heuristic gets to overrule it. */
    private var usernameIsCertain = false
    private var previousText: Field? = null

    fun visit(node: ViewNode) {
        if (webDomain == null) {
            webDomain = node.webDomain?.takeIf(String::isNotEmpty)
        }
        node.asField()?.let { field ->
            when (node.kind()) {
                Kind.PASSWORD -> {
                    passwords += field
                    if (username == null) username = previousText
                }

                Kind.USERNAME -> if (!usernameIsCertain) {
                    username = field
                    usernameIsCertain = true
                }

                Kind.OTHER -> previousText = field
            }
        }
        for (index in 0 until node.childCount) visit(node.getChildAt(index))
    }
}

private fun ViewNode.asField(): Field? {
    val id = autofillId ?: return null
    if (autofillType != View.AUTOFILL_TYPE_TEXT) return null
    return Field(id = id, value = currentText())
}

/**
 * What the field holds right now. On a fill request the value arrives as an `AutofillValue`; on a
 * save request, and in browsers running in compatibility mode, only the node text is set.
 */
private fun ViewNode.currentText(): String? {
    val value = autofillValue
    val typed = if (value != null && value.isText) value.textValue?.toString() else text?.toString()
    return typed?.takeIf(String::isNotEmpty)
}

private fun ViewNode.kind(): Kind {
    autofillHints?.filterNotNull()?.forEach { hint ->
        if (PASSWORD_HINTS.any { it.equals(hint, ignoreCase = true) }) return Kind.PASSWORD
        if (USERNAME_HINTS.any { it.equals(hint, ignoreCase = true) }) return Kind.USERNAME
    }
    if (isPasswordInput()) return Kind.PASSWORD
    val signature = signature()
    return when {
        PASSWORD_TOKENS.any { it in signature } -> Kind.PASSWORD
        USERNAME_TOKENS.any { it in signature } -> Kind.USERNAME
        else -> Kind.OTHER
    }
}

/** Everything the field says about itself, flattened into one string to look for keywords in. */
private fun ViewNode.signature(): String = buildString {
    idEntry?.let { append(it).append(' ') }
    hint?.let { append(it).append(' ') }
    htmlInfo?.attributes?.forEach { attribute ->
        val name = attribute?.first ?: return@forEach
        if (name.lowercase() in HTML_ATTRIBUTES) append(attribute.second).append(' ')
    }
}.lowercase()

private fun ViewNode.isPasswordInput(): Boolean {
    val variation = inputType and InputType.TYPE_MASK_VARIATION
    return when (inputType and InputType.TYPE_MASK_CLASS) {
        InputType.TYPE_CLASS_TEXT -> variation in TEXT_PASSWORD_VARIATIONS
        InputType.TYPE_CLASS_NUMBER -> variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD
        else -> false
    }
}

private val PASSWORD_HINTS = listOf(
    View.AUTOFILL_HINT_PASSWORD,
    "passwordAuto",
    "current-password",
    "new-password"
)

private val USERNAME_HINTS = listOf(
    View.AUTOFILL_HINT_USERNAME,
    View.AUTOFILL_HINT_EMAIL_ADDRESS,
    "email",
    "user-name",
    "login"
)

private val PASSWORD_TOKENS =
    listOf("password", "passwd", "pwd", "contraseñ", "contrasen", "clave")

private val USERNAME_TOKENS =
    listOf("username", "user", "email", "e-mail", "correo", "usuario", "login", "identifier")

private val HTML_ATTRIBUTES =
    setOf("type", "name", "id", "autocomplete", "placeholder", "aria-label", "label")

private val TEXT_PASSWORD_VARIATIONS = setOf(
    InputType.TYPE_TEXT_VARIATION_PASSWORD,
    InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
    InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
)
