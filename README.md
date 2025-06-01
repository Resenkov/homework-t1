## Полноценное общение между двумя сервисами.

*DataInitializer создает тестовые транзакции и отправляет их как сообщения.*

*TransactionListenerService делает первичную обработку сообщений и отправку в t1_demo_transaction_accept, а так же окончательную обработку прослушивая топик t1_demo_transaction_result*

*AcceptedTransactionListenerService обрабатывает сообщения из топика t1_demo_transaction_accept и отправляет в топик t1_demo_transaction_result*

Для запуска и адекватной проверки нужна PostgreSQL. Её образ и образ кафки с зукипером прикреплены в docker-compose.yml.
