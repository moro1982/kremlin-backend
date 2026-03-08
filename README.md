### KREMLIN Online - Notas de desarrollo ###

## Introducción ##

Este proyecto en su estado actual forma parte de un proyecto de largo aliento, consistente en el desarrollo de una versión del juego de mesa Kremlin en versión online.

A los fines de cumplir lo requerido para el examen final de Taller de Programación IV, he decidido completar el desarrollo sólo hasta una etapa inicial del juego, para mostrar las tecnologías utilizadas y su funcionamiento en tiempo real.

## Breve reseña del juego ##

Kremlin es un juego de mesa, de 3 a 6 jugadores, que representa una sátira de la política durante la Unión Soviética. El juego tuvo una primera versión en 1986, cuando la URSS existía, reimprimiéndose pocas veces en años posteriores.

En el juego, cada jugador representa una facción dentro del Partido, la cual debe influir sobre los políticos que se ubican al azar en la pirámide del Politburó, con el fin de eventualmente controlar al máximo Líder del partido. El jugador que lo logre en 3 turnos (consecutivos o no), gana el juego. Existen, además, otras condiciones de victoria, que por el momento no se implementarán.

El juego es bastante complejo, ya que posee muchas reglas que deben cumplirse a lo largo de 10 turnos, cada uno de ellos con 8 Fases, en las que se pueden realizar diferentes acciones. Uno a uno, los políticos que ocupan los diferentes cargos en el Politburó podrán realizar las acciones que dicho cargo les habilita.

Para controlar a los diferentes políticos, al inicio del juego (y antes de repartir los políticos en el Politburó) los jugadores ASIGNAN secretamente un valor de influencia sobre 10 políticos de su elección de entre un total de 26. Los valores a asignar van del 1 al 10, y no pueden repetirse. De esta forma, cada jugador tendrá un político con influencia asignada 1, otro con 2, etc.

Cuando todos terminan de asignar su influencia en 10 políticos, comienza el juego, iniciándose el turno 1, fase 1. A partir de este momento, los jugadores pueden DECLARAR el valor de influencia asignada sobre un político, a fin de controlarlo. Si otro jugador también tiene influencia asignada sobre el mismo político, puede a su vez declarar un valor de influencia mayor, y el control pasa a ser suyo.

Cabe aclarar que no se puede declarar más influencia de la que se ha asignado, pero sí puede declararse un valor menor, para luego incrementarlo oportunamente. Esto se utiliza tácticamente, para no perder toda la influencia por acciones posteriores que puedan eliminarla.

Hasta aquí, la explicación de las reglas necesarias para comprender lo desarrollado hasta el momento.

## Descripción del desarrollo del backend ##

El backend se realizó estructurando el motor del juego en 3 grandes pilares: Juego, Fases y Acciones.

- En lo referente al Juego, involucra los aspectos más generales, como el estado global de una partida, el manejo de las mismas, la inscripción de un jugador a una partida existente, la creación de una nueva, etc. Asimismo, gobierna el flujo general del juego, y delega en PhaseManager las indicaciones de avances de fase, etc. Todo esto se maneja desde la clase de servicio GameService.

- En lo referente a las Fases, disponemos de un PhaseManager como clase de servicio, la cual se encarga de controlar el flujo de fases, resolución de las mismas, y delega la resolución de acciones pendientes en ActionService. Maneja también los eventos automáticos de fase, así como el flujo de sub-fases implícitas correspondientes a acciones más complejas, como Purgas y Juicios.

- En lo referente a las Acciones que anuncian los jugadores, la clase de servicio ActionService gobierna todas y cada una de las acciones existentes en el juego, así como la resolución de la cola de acciones que no se hayan resuelto inmediatamente. Es una clase bastante robusta y compleja, pero su funcionamiento no es tan difícil. En términos generales, un jugador Anuncia una acción, la cual se valida (si el estado global o particular lo permite, si es la fase correcta, etc.) y pasa a ejecutarse. Según el tipo de acción, esta se resolverá inmediatamente (como las Declaraciones de Influencia) o se mantendrá en espera de posibles acciones de respuesta (como las Purgas y los Escapes al Exilio).

El sistema posee persistencia de todas las acciones realizadas y del estado del juego, así como de los Politicos, Jugadores, etc. Además, posee un servicio de Notificaciones de las acciones que generen un cambio en algún estado.

## Sistema de autenticación y Notificaciones SSE ##

La autenticación se realizó con JWT, de acuerdo a lo solicitado por la cátedra, incorporándose a la autenticación con usuario y contraseña.

Por otro lado, se agregó el envío de notificaciones vía SSE (Server-Sent Events), lo cual trajo algunas complicaciones por la incompatibilidad de SSE con la autenticación JWT, ya que SSE se maneja con cookies y no lee la cabecera "Authorization Bearer". Esto lo pudimos subsanar creando, luego de la autenticación via JWT, un "handshake" con una sesión especial para los eventos de notificación.

