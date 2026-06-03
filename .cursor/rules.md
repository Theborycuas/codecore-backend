# CodeCore Backend - Cursor Rules

## Regla 1

Nunca asumir que una implementación funciona.

Antes de declarar una tarea como completada se debe ejecutar validación real.

Durante el desarrollo:

1. Preferir validaciones rápidas:
   - compileJava
   - test individual afectado
   - módulo específico afectado

2. Evitar ejecutar:
   - ./gradlew build
   - suites completas
   después de cada cambio pequeño.

3. Ejecutar obligatoriamente validación completa únicamente al finalizar la tarea.

Validación mínima final:

```bash
./gradlew build

---

## Otra regla que añadiría para CodeCore

Porque veo exactamente el patrón que te está ralentizando.

```markdown
## Regla 1.1

Durante desarrollo iterativo:

NO ejecutar:

```bash
./gradlew --stop
./gradlew --no-daemon

---

## Regla 2

No declarar:

* COMPLETADO
* DONE
* BUILD SUCCESSFUL
* IMPLEMENTADO

sin evidencia obtenida mediante ejecución real.

---

## Regla 3

Toda documentación debe escribirse en archivos .md.

No generar informes extensos en el chat.

El chat debe contener únicamente:

* resumen ejecutivo
* archivos modificados
* comandos ejecutados
* resultado final

---

## Regla 4

Los informes técnicos deben almacenarse en:

docs/audits/

Formato:

PASO-XX-NOMBRE.md

---

## Regla 5

Antes de implementar:

1. Revisar documentación en codecore-specifications.
2. Detectar inconsistencias.
3. Actualizar documentación únicamente si es necesario.

---

## Regla 6

No introducir:

* microservicios
* CQRS
* Event Sourcing
* Saga
* Kubernetes
* API Gateway

salvo instrucción explícita.

---

## Regla 7

No crear clases adicionales sin justificar su necesidad.

Priorizar la solución más simple.

---

## Regla 8

Todo cambio debe mantener:

DDD
Hexagonal Architecture
Modular Monolith

como principios obligatorios.

---

## Regla 9

Nunca generar documentación duplicada.

Si existe un archivo .md apropiado:

actualizarlo.

No crear otro.
