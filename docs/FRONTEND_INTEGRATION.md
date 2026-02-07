# Guía de Integración Frontend (Angular)

Esta guía está dirigida al equipo de frontend para integrar la aplicación Angular con el backend LiveMenu.

## Configuración Base

### URL del Backend

**Desarrollo:**
```
http://localhost:8080
```

**Producción:**
```
https://api.tudominio.com
```

### Configuración CORS

El backend está configurado para aceptar peticiones desde:
- `http://localhost:4200` (Angular dev server por defecto)
- `http://localhost:3000` (alternativa)

Para producción, configura la variable de entorno `CORS_ORIGINS` en el backend con los dominios permitidos.

## Autenticación

### Flujo de Autenticación

1. **Registro de Usuario**
   ```
   POST /api/v1/auth/register
   Content-Type: application/json
   
   {
     "email": "usuario@example.com",
     "password": "password123"
   }
   ```

2. **Login**
   ```
   POST /api/v1/auth/login
   Content-Type: application/json
   
   {
     "email": "usuario@example.com",
     "password": "password123"
   }
   
   Response:
   {
     "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
     "refresh_token": "...",
     "expires_in": 300
   }
   ```

3. **Uso del Token**
   - Guarda el `access_token` en localStorage o sessionStorage
   - Incluye el token en todas las peticiones protegidas:
     ```
     Authorization: Bearer {access_token}
     ```

4. **Obtener Usuario Actual**
   ```
   GET /api/v1/auth/user
   Authorization: Bearer {access_token}
   ```

### Manejo de Tokens en Angular

```typescript
// Ejemplo de interceptor para agregar token
@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    const token = localStorage.getItem('access_token');
    
    if (token) {
      req = req.clone({
        setHeaders: {
          Authorization: `Bearer ${token}`
        }
      });
    }
    
    return next.handle(req);
  }
}
```

## Endpoints por Módulo

### 1. Autenticación (`/api/v1/auth`)

| Método | Endpoint | Auth | Descripción |
|--------|----------|------|-------------|
| POST | `/register` | No | Registrar nuevo usuario |
| POST | `/login` | No | Iniciar sesión |
| POST | `/logout` | Sí | Cerrar sesión |
| GET | `/user` | Sí | Obtener usuario actual |

### 2. Restaurantes (`/api/v1/admin/restaurants`)

| Método | Endpoint | Auth | Descripción |
|--------|----------|------|-------------|
| GET | `/` | Sí | Listar restaurantes del usuario |
| GET | `/{id}` | Sí | Obtener restaurante por ID |
| POST | `/` | Sí | Crear restaurante |
| PUT | `/{id}` | Sí | Actualizar restaurante |
| DELETE | `/{id}` | Sí | Eliminar restaurante |

**Ejemplo de creación:**
```json
{
  "name": "Mi Restaurante",
  "description": "Descripción del restaurante",
  "logo": "https://...",
  "phone": "+1234567890",
  "address": "Calle 123",
  "schedule": {
    "monday": {"open": "09:00", "close": "22:00", "closed": false},
    ...
  }
}
```

### 3. Categorías (`/api/v1/admin/restaurants/{restaurantId}/categories`)

| Método | Endpoint | Auth | Descripción |
|--------|----------|------|-------------|
| GET | `/` | Sí | Listar categorías |
| GET | `/{id}` | Sí | Obtener categoría |
| POST | `/` | Sí | Crear categoría |
| PUT | `/{id}` | Sí | Actualizar categoría |
| DELETE | `/{id}` | Sí | Eliminar categoría |
| PATCH | `/reorder` | Sí | Reordenar categorías |

### 4. Platos (`/api/v1/admin/restaurants/{restaurantId}/dishes`)

| Método | Endpoint | Auth | Descripción |
|------|----------|------|-------------|
| GET | `/` | Sí | Listar platos (con filtros opcionales) |
| GET | `/{id}` | Sí | Obtener plato |
| POST | `/` | Sí | Crear plato (JSON) |
| POST | `/with-image` | Sí | Crear plato con imagen (multipart) |
| PUT | `/{id}` | Sí | Actualizar plato (JSON) |
| PUT | `/{id}/with-image` | Sí | Actualizar plato con imagen (multipart) |
| DELETE | `/{id}` | Sí | Eliminar plato |
| PATCH | `/{id}/availability` | Sí | Toggle disponibilidad |

**Filtros en GET:**
- `?categoryId={uuid}` - Filtrar por categoría
- `?available=true` - Solo platos disponibles

### 5. Imágenes (`/api/v1/images`)

| Método | Endpoint | Auth | Descripción |
|--------|----------|------|-------------|
| POST | `/upload` | Sí | Subir imagen (multipart/form-data) |
| GET | `/{variant}/{filename}` | No | Obtener imagen (redirect a GCS) |

**Upload de imagen:**
```typescript
const formData = new FormData();
formData.append('image', file);

this.http.post('/api/v1/images/upload', formData, {
  headers: { Authorization: `Bearer ${token}` }
}).subscribe(response => {
  // response.originalUrl, response.thumbnailUrl, 
  // response.mediumUrl, response.largeUrl
});
```

### 6. Menú Público (`/api/v1/public/menu`)

| Método | Endpoint | Auth | Descripción |
|--------|----------|------|-------------|
| GET | `/{slug}` | No | Obtener menú público por slug |

**Ejemplo de respuesta:**
```json
{
  "restaurant": {
    "id": "...",
    "name": "Mi Restaurante",
    "slug": "mi-restaurante",
    "logo": "https://...",
    ...
  },
  "categories": [
    {
      "id": "...",
      "name": "Entradas",
      "description": "...",
      "position": 1,
      "dishes": [
        {
          "id": "...",
          "name": "Ensalada César",
          "price": 12.99,
          "offerPrice": 9.99,
          "imageUrl": "https://...",
          "available": true,
          "featured": true,
          "tags": ["vegetarian"],
          ...
        }
      ]
    }
  ]
}
```

**Ruta en Angular:**
```
/m/{slug}
```

### 7. Código QR (`/api/v1/admin/restaurants/{restaurantId}/qr`)

| Método | Endpoint | Auth | Descripción |
|--------|----------|------|-------------|
| GET | `/` | Sí | Obtener info del QR (preview) |
| GET | `/download` | Sí | Descargar QR generado |

**Parámetros de descarga:**
- `?size=S|M|L|XL` - Tamaño del QR (default: M)
- `?format=PNG|SVG` - Formato (default: PNG)
- `?includeLogo=true|false` - Incluir logo (default: false)

**Ejemplo:**
```typescript
// Obtener info para preview
this.http.get(`/api/v1/admin/restaurants/${id}/qr`, {
  headers: { Authorization: `Bearer ${token}` }
}).subscribe(qrInfo => {
  // qrInfo.qrUrl, qrInfo.publicMenuUrl, etc.
});

// Descargar QR
const url = `/api/v1/admin/restaurants/${id}/qr/download?size=L&format=PNG&includeLogo=true`;
window.open(url, '_blank');
```

## Manejo de Errores

### Códigos de Estado HTTP

- `200 OK` - Operación exitosa
- `201 Created` - Recurso creado
- `204 No Content` - Operación exitosa sin contenido
- `400 Bad Request` - Error de validación
- `401 Unauthorized` - Token inválido o faltante
- `403 Forbidden` - Sin permisos
- `404 Not Found` - Recurso no encontrado
- `500 Internal Server Error` - Error del servidor

### Formato de Errores

```json
{
  "error": "Mensaje de error descriptivo"
}
```

### Manejo en Angular

```typescript
this.http.get('/api/v1/...').subscribe({
  next: (data) => { /* éxito */ },
  error: (error) => {
    if (error.status === 401) {
      // Token expirado, redirigir a login
      this.router.navigate(['/login']);
    } else if (error.status === 403) {
      // Sin permisos
      this.showError('No tienes permisos para esta acción');
    } else {
      // Otro error
      this.showError(error.error?.error || 'Error desconocido');
    }
  }
});
```

## Variables de Entorno Recomendadas

Crea un archivo `environment.ts` en Angular:

```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080',
  publicMenuBaseUrl: 'http://localhost:8080/m'
};

export const environmentProd = {
  production: true,
  apiUrl: 'https://api.tudominio.com',
  publicMenuBaseUrl: 'https://tudominio.com/m'
};
```

## Servicios Angular Recomendados

### 1. Servicio de Autenticación

```typescript
@Injectable({ providedIn: 'root' })
export class AuthService {
  private apiUrl = environment.apiUrl;
  
  login(email: string, password: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/api/v1/auth/login`, {
      email, password
    });
  }
  
  register(email: string, password: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/api/v1/auth/register`, {
      email, password
    });
  }
  
  getCurrentUser(): Observable<User> {
    return this.http.get<User>(`${this.apiUrl}/api/v1/auth/user`);
  }
}
```

### 2. Servicio de Restaurantes

```typescript
@Injectable({ providedIn: 'root' })
export class RestaurantService {
  private apiUrl = environment.apiUrl;
  
  getRestaurants(): Observable<Restaurant[]> {
    return this.http.get<Restaurant[]>(`${this.apiUrl}/api/v1/admin/restaurants`);
  }
  
  getRestaurant(id: string): Observable<Restaurant> {
    return this.http.get<Restaurant>(`${this.apiUrl}/api/v1/admin/restaurants/${id}`);
  }
  
  createRestaurant(data: CreateRestaurantRequest): Observable<Restaurant> {
    return this.http.post<Restaurant>(`${this.apiUrl}/api/v1/admin/restaurants`, data);
  }
}
```

### 3. Servicio de Menú Público

```typescript
@Injectable({ providedIn: 'root' })
export class PublicMenuService {
  private apiUrl = environment.apiUrl;
  
  getPublicMenu(slug: string): Observable<PublicMenuResponse> {
    return this.http.get<PublicMenuResponse>(
      `${this.apiUrl}/api/v1/public/menu/${slug}`
    );
  }
}
```

## Interceptores Recomendados

### 1. Interceptor de Autenticación

```typescript
@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    const token = localStorage.getItem('access_token');
    
    if (token) {
      req = req.clone({
        setHeaders: { Authorization: `Bearer ${token}` }
      });
    }
    
    return next.handle(req).pipe(
      catchError((error: HttpErrorResponse) => {
        if (error.status === 401) {
          // Token expirado
          localStorage.removeItem('access_token');
          // Redirigir a login
        }
        return throwError(() => error);
      })
    );
  }
}
```

### 2. Interceptor de Errores

```typescript
@Injectable()
export class ErrorInterceptor implements HttpInterceptor {
  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    return next.handle(req).pipe(
      catchError((error: HttpErrorResponse) => {
        let errorMessage = 'Error desconocido';
        
        if (error.error?.error) {
          errorMessage = error.error.error;
        } else if (error.message) {
          errorMessage = error.message;
        }
        
        // Mostrar notificación de error
        console.error('API Error:', errorMessage);
        
        return throwError(() => error);
      })
    );
  }
}
```

## Rutas Públicas

### Ruta del Menú Público

```
/m/:slug
```

Esta ruta debe:
1. Obtener el slug de la URL
2. Llamar a `GET /api/v1/public/menu/{slug}`
3. Renderizar el menú con:
   - Header con logo y nombre del restaurante
   - Navegación por categorías
   - Lista de platos por categoría
   - Indicadores visuales (disponibilidad, etiquetas)

## Testing

### Ejemplo de Prueba con Postman

La colección de Postman incluye todos los endpoints y ejemplos. Úsala como referencia para las peticiones.

### Mock Data para Desarrollo

Puedes usar los endpoints públicos sin autenticación para desarrollo:
- `GET /api/v1/public/menu/{slug}` - No requiere token

## Notas Importantes

1. **Tokens JWT**: Los tokens tienen un tiempo de expiración. Implementa refresh token o redirige a login cuando expire.

2. **CORS**: En producción, asegúrate de configurar `CORS_ORIGINS` en el backend con los dominios correctos.

3. **Imágenes**: Las imágenes se sirven directamente desde GCP Storage. Las URLs retornadas son públicas y no requieren autenticación.

4. **Caché del Menú Público**: El menú público está cacheado en el backend. Los cambios pueden tardar unos segundos en reflejarse.

5. **Multipart Upload**: Para subir imágenes, usa `FormData` y `multipart/form-data`.

## Soporte

Para dudas o problemas, consulta:
- Colección de Postman: `postman/LiveMenu.postman_collection.json`
- Documentación del backend: `docs/`
- Logs del backend para debugging

