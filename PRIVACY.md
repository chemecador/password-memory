# Password Memory Privacy Policy

Last updated: July 26, 2026

Password Memory (`com.chemecador.passwordmemory`) is a local-first Android password manager. It has no user accounts, developer-operated servers, analytics, advertising, or network permission. The developer does not collect the vault, and the app does not automatically transmit, sell, or share it.

Password Memory is developed and published by Chemecador (Alejandro Gata Gascón), Spain.

## Data handled on the device

Password Memory handles the service names, usernames, passwords, hints, categories, and favorites entered by the user. Its autofill feature also handles the package name or claimed web domain of the requesting app and the credentials the user chooses to fill or save.

Authentication is performed by Android. Password Memory receives only whether the authentication succeeded; it never receives or stores biometric data, a device PIN, or a device pattern.

## Local disclosures and user-directed transfers

Data remains in the app's private local storage unless the user explicitly:

- fills credentials into another app through Android Autofill;
- copies a value to the Android clipboard;
- imports a CSV file selected with the system file picker; or
- exports an encrypted backup to a user-selected location.

Password Memory hands the selected data to the Android component or destination chosen by the user. The destination app, Android clipboard or keyboard services, and the selected document provider may process, retain, or synchronize that data under their own policies. A document provider may, for example, save an exported backup in cloud storage. Password Memory does not choose those destinations, contact them over the network itself, or copy imported CSV files into its private storage. It does not use Android cloud backup or device-to-device backup.

## Security

Recoverable passwords are encrypted with AES-256-GCM using a key held by Android Keystore and gated by strong device authentication. Hardware backing depends on the device. Irrecoverable passwords are stored only as salted SHA-256 hashes. Screen capture is blocked. Clipboard values are marked sensitive and can be cleared automatically. User-created backup files are encrypted with the passphrase selected during export.

## Retention and deletion

Vault entries remain until the user deletes them, clears the app's data, or uninstalls the app. Deleting an entry removes it from the local database.

Backup files and plaintext CSV files are stored outside the app and remain until the user deletes them from the selected location. After a CSV import, the app offers to delete the source file. Clipboard contents follow the clear delay chosen in Settings.

Because Password Memory has no accounts or servers, there is no remote account data to request or delete.

## Contact and policy changes

For privacy questions, email <chemecador@gmail.com> or open an issue at <https://github.com/chemecador/password-memory/issues>.

Material changes will be reflected in the in-app policy and this public policy before a release that changes data handling.

---

# Política de privacidad de Password Memory

Última actualización: 26 de julio de 2026

Password Memory (`com.chemecador.passwordmemory`) es un gestor de contraseñas para Android diseñado para funcionar en local. No tiene cuentas de usuario, servidores operados por el desarrollador, analítica, publicidad ni permiso de red. El desarrollador no recopila la caja fuerte y la app no la transmite automáticamente, vende ni comparte.

Password Memory está desarrollada y publicada por Chemecador (Alejandro Gata Gascón), España.

## Datos tratados en el dispositivo

Password Memory trata los nombres de servicio, usuarios, contraseñas, pistas, categorías y favoritos introducidos por el usuario. Autocompletar también trata el nombre de paquete o dominio web declarado por la app solicitante y las credenciales que el usuario decide rellenar o guardar.

Android realiza la autenticación. Password Memory solo recibe si la autenticación ha tenido éxito; nunca recibe ni almacena datos biométricos, el PIN ni el patrón del dispositivo.

## Tratamiento local y transferencias iniciadas por el usuario

Los datos permanecen en el almacenamiento local privado de la app salvo que el usuario decida expresamente:

- rellenar credenciales en otra app mediante Autocompletar de Android;
- copiar un valor al portapapeles de Android;
- importar un CSV elegido con el selector de archivos del sistema; o
- exportar una copia cifrada a una ubicación elegida por el usuario.

Password Memory entrega los datos elegidos al componente de Android o destino seleccionado por el usuario. La app de destino, los servicios de portapapeles o teclado de Android y el proveedor de documentos elegido pueden tratar, conservar o sincronizar esos datos conforme a sus propias políticas. Por ejemplo, un proveedor de documentos puede guardar una copia exportada en la nube. Password Memory no elige esos destinos, no contacta con ellos por red por sí misma y no copia los CSV importados a su almacenamiento privado. Tampoco usa las copias en la nube ni entre dispositivos de Android.

## Seguridad

Las contraseñas recuperables se cifran con AES-256-GCM mediante una clave conservada por Android Keystore y condicionada a una autenticación fuerte del dispositivo. El respaldo por hardware depende del dispositivo. Las contraseñas irrecuperables solo se almacenan como hashes SHA-256 con sal. Las capturas están bloqueadas. Los valores copiados se marcan como sensibles y pueden borrarse automáticamente. Las copias creadas por el usuario se cifran con la contraseña elegida al exportar.

## Conservación y eliminación

Las entradas permanecen hasta que el usuario las elimina, borra los datos de la app o desinstala la app. Eliminar una entrada la borra de la base de datos local.

Las copias y los CSV en texto plano se guardan fuera de la app y permanecen hasta que el usuario los elimina de la ubicación seleccionada. Tras importar un CSV, la app ofrece borrar el fichero original. El portapapeles sigue el retraso elegido en Ajustes.

Como Password Memory no tiene cuentas ni servidores, no existe información remota de cuenta que solicitar o eliminar.

## Contacto y cambios

Para cuestiones de privacidad, escribe a <chemecador@gmail.com> o abre una incidencia en <https://github.com/chemecador/password-memory/issues>.

Los cambios importantes se reflejarán en la política incluida en la app y en esta política pública antes de publicar una versión que cambie el tratamiento de datos.
