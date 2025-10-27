NOTE: This project is in development process for now and can contain some bugs/errors, and it is not production ready in any way. Use it on your own risk.

== Review

This is the second application - order processing app. Use Control panel view to start requests handling. Check the steps below to start entire project correctly.

=== How to start project

Logically, this part is a part with all logic and processes, but, in fact, should be started first.

* Start `RabbitMQ`

[source, bash]
----
docker run -d --hostname my-rabbit --name some-rabbit -p 5672:5672 -p 15672:15672 rabbitmq:3-management
----

* Start application
* Go to http://localhost:8282/control-panel-view and press Reset button. You must do this even if this is the first launch of application - some RabbitMQ logic is connected to this button.
* Go to http://localhost:8282/numerators and create two entries - `order` - 0 and `delivery` - 0. Those are going to be used to create Business key sequences. Names must be as present, but numbers are up to you, it is just a starting seed.
* Go back to http://localhost:8282/control-panel-view and start orders handling.

NOTE: Pay attention to files in `src/main/resources/processes` - those are processes that deployed on application startup, so application working correctly. If there are no correct processes in folder - exceptions will appear.

NOTE: Webinar script was added to this project, so you can remove all the processes from server and check them step-by-step.