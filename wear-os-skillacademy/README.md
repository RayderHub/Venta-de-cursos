# SkillAcademy Wear OS

App nativa minima para Wear OS que consume promociones desde el backend Fastify y muestra notificaciones reales en el reloj/emulador.

## Backend

Desde la raiz del proyecto:

```powershell
cd backend-fastify
npm.cmd start
```

Endpoint para el reloj:

```txt
http://10.0.2.2:3000/api/widget/wearable/promociones
```

En un emulador Android/Wear, `10.0.2.2` apunta a tu PC.

## Android Studio

1. Abre Android Studio.
2. File > Open.
3. Selecciona la carpeta `wear-os-skillacademy`.
4. Elige tu emulador Wear OS.
5. Run.
6. Acepta el permiso de notificaciones en el reloj.
7. Toca `Sincronizar promociones`.
8. Toca `Mostrar notificaciones`.

## Dispositivo Wear OS fisico

Opcion A, misma red Wi-Fi:

1. Mantén el backend en `0.0.0.0:3000`.
2. Busca la IP de tu PC.
3. En la app del reloj cambia la URL a:

```txt
http://IP_DE_TU_PC:3000/api/widget/wearable/promociones
```

Opcion B, ADB reverse:

```powershell
adb devices
adb -s SERIAL_DEL_RELOJ reverse tcp:3000 tcp:3000
```

Luego usa esta URL en la app:

```txt
http://127.0.0.1:3000/api/widget/wearable/promociones
```

## Produccion (backend en Render)

Cuando el backend este desplegado en Render, el reloj debe apuntar a la URL publica del servicio:

```txt
https://skillacademy-backend.onrender.com/api/widget/wearable/promociones
```

En el codigo la URL por defecto esta en `PromoService.java` (constante `DEFAULT_URL`). Puedes:

- Cambiarla ahi y recompilar, o
- Cambiarla en tiempo de ejecucion con `PromoService.setApiUrl(context, url)` (la guarda en `SharedPreferences`).

Nota: la app usa la URL guardada en `SharedPreferences` si existe; si no, usa `DEFAULT_URL`.
