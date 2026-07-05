## Пет проект для изучения ZIO redis
### Сборка
java 11 и выше

### Тестирование
Для локального запуска поднять redis в docker командой docker compose up из директории

дебаггинг из консоли docker-сервиса:
1) redis-cli
2) 127.0.0.1:6379> keys *
"myList"
"myHash"
"mySet"
3) 127.0.0.1:6379> get hello
"world"
