# CodeCore Backend Architecture

## Objetivo

CodeCore es una plataforma enterprise reusable diseñada para servir como núcleo tecnológico de múltiples soluciones SaaS:

- Salud humana
- Veterinarias
- Odontología
- Psicología
- Clínicas
- Gestión organizacional
- Plataformas multi-tenant

El objetivo es construir una plataforma mantenible, escalable y evolutiva sin introducir complejidad distribuida prematuramente.

---

# Principios Arquitectónicos

## Modular Monolith

CodeCore NO es una arquitectura de microservicios.

Todo el sistema se ejecuta inicialmente en:

- Una JVM
- Una aplicación Spring Boot
- Una base PostgreSQL
- Una instancia Redis

Los bounded contexts viven dentro del monolito como módulos independientes.

---

## DDD (Domain Driven Design)

La organización funcional se basa en bounded contexts.

Cada contexto encapsula:

- Dominio
- Casos de uso
- Persistencia
- Adaptadores

Los módulos se comunican mediante contratos explícitos.

---

## Hexagonal Architecture

Cada bounded context sigue el modelo:

Domain
↓
Application
↓
Ports
↓
Adapters

El dominio nunca depende de:

- Spring
- PostgreSQL
- Redis
- Kafka
- Frameworks externos

---

## Event-Driven Ready

CodeCore está preparado para eventos de dominio.

Sin embargo:

- Kafka NO es obligatorio para el dominio.
- Los eventos nacen dentro del dominio.
- La infraestructura decide cómo publicarlos.

Actualmente Kafka no forma parte del flujo funcional principal.

---

## Reactive First

Toda la plataforma se construye utilizando:

- Spring WebFlux
- Project Reactor
- R2DBC

Se evita programación bloqueante dentro de:

- Casos de uso
- Repositorios
- Adaptadores reactivos

---

# Stack Oficial

## Backend

- Java 17
- Spring Boot 3
- Spring WebFlux
- Spring Data R2DBC

## Base de Datos

- PostgreSQL

## Cache

- Redis

## Mensajería

- Kafka (futuro)

## Observabilidad

- OpenTelemetry

## Migraciones

- Flyway

## Build System

- Gradle Kotlin DSL

## Infraestructura

- Docker

---

# Estructura Física del Repositorio

```text
codecore-backend/

apps/
modules/
platform/
shared/
infrastructure/
buildSrc/


apps/

Contiene aplicaciones ejecutables.

Actualmente:

apps/
└── codecore-api

Responsabilidad:

Composition Root
Bootstrap Runtime
Configuración global
Wiring de módulos

NO contiene lógica de negocio.

modules/

Contiene bounded contexts reales.

Ejemplos:

modules/

identity-access-management
tenant-management
user-management
authorization-management
audit-management
notification-management
file-management
subscription-management
billing-management
payment-management
configuration-management
observability-management
integration-management

Cada módulo representa una capacidad de negocio.

platform/

Capacidades técnicas compartidas.

Ejemplos:

platform/

platform-postgres
platform-r2dbc
platform-webflux
platform-redis
platform-kafka
platform-security
platform-telemetry

No contienen reglas de negocio.

shared/

Componentes compartidos entre bounded contexts.

shared/

shared-kernel
shared-events
shared-test

Deben mantenerse pequeños y estables.

infrastructure/

Infraestructura de desarrollo.

Ejemplos:

infrastructure/

docker
scripts
Bounded Contexts Actuales
Identity Access Management

Responsable de:

Registro de identidades
Credenciales
Autenticación
Estados de identidad

No administra perfiles de usuario.

Tenant Management

Responsable de:

Tenants
Aislamiento multi-tenant
Configuración por tenant
User Management

Responsable de:

Perfil de usuario
Datos personales
Información de contacto
Estrategia de Persistencia
Base de Datos

Actualmente:

1 PostgreSQL
1 Base de datos

No existe separación física por módulo.

El aislamiento se realiza mediante:

Schemas
Convenciones
Reglas de acceso
Flyway

Flyway es la única fuente oficial de verdad para cambios de esquema.

Toda modificación de base de datos debe realizarse mediante migraciones versionadas.

Ejemplo:

V1__create_iam_schema.sql
V2__create_iam_user_table.sql
V3__cleanup_iam_user.sql

Nunca modificar estructuras manualmente.

Multi-Tenancy

Regla obligatoria:

Toda información de negocio debe estar asociada a:

tenant_id

Las consultas deben respetar aislamiento por tenant.

Testing Strategy
Unit Tests

Validan:

Dominio
Casos de uso
Value Objects

Sin infraestructura real.

Integration Tests

Validan:

PostgreSQL real
Flyway real
Repositorios reales

Utilizando:

Testcontainers

No utilizar:

H2
Embedded Databases
Decisiones Arquitectónicas Importantes
No Microservices

No introducir:

Service Discovery
Kubernetes
API Gateway distribuido
Comunicación entre servicios

sin una necesidad demostrada.

No Overengineering

Evitar:

Abstracciones prematuras
Patrones innecesarios
Frameworks internos

La solución más simple que funcione correctamente es la preferida.

Documentación

La documentación es parte del sistema.

Antes de implementar:

Revisar especificaciones.
Verificar alineamiento.
Actualizar documentación si es necesario.
Implementar.
Validar mediante pruebas.
Estado Actual

Completado:

Docker Foundation
PostgreSQL Foundation
Redis Foundation
Flyway Foundation
R2DBC Foundation
IAM Persistence Model
IAM Persistence Adapter
RegisterIdentityUseCase
Persistence Integration Tests

El desarrollo debe continuar respetando las reglas descritas en este documento.


Con este archivo, cualquier instancia de Cursor que entre al repositorio tendrá el contexto arquitectónico suficiente par