package ru.practicum.moviehub.http;

public enum RequestType {
    //Получение всех фильмов
    GET,
    //Добавление фильма
    POST,
    //Получение фильма по идентификатору
    GET_ID,
    //Удаление фильма
    DELETE_ID,
    //Фильтрация по году выпуска
    GET_YEAR,
    //Такого эндпоинта не существует
    NO_ENDPOINT

}
