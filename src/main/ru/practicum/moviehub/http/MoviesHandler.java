package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class MoviesHandler extends BaseHttpHandler {
    private final MoviesStore moviesStore;
    private final Gson gson;

    public MoviesHandler(MoviesStore moviesStore) {
        this.moviesStore = moviesStore;
        this.gson = new Gson();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        switch (resolveRequestType(exchange)) {
            case GET -> get(exchange);
            case GET_ID -> getId(exchange);
            case GET_YEAR -> getYear(exchange);
            case POST -> post(exchange);
            case DELETE_ID -> delete(exchange);
            case NO_ENDPOINT -> sendJson(exchange, 405, "Method Not Allowed");
        }
    }

    private RequestType resolveRequestType(HttpExchange exchange) {
        String path = exchange.getRequestURI().getPath();
        String query = exchange.getRequestURI().getRawQuery();
        switch (exchange.getRequestMethod().toUpperCase()) {
            case "GET":
                if (path.equals("/movies")) {
                    if (query != null && query.startsWith("year=")) {
                        return RequestType.GET_YEAR;
                    }
                    return RequestType.GET;
                } else if (path.startsWith("/movies/")) {
                    return RequestType.GET_ID;
                }
                return RequestType.NO_ENDPOINT;
            case "POST":
                if (path.equals("/movies")) {
                    return RequestType.POST;
                }
                return RequestType.NO_ENDPOINT;
            case "DELETE":
                if (path.startsWith("/movies/")) {
                    return RequestType.DELETE_ID;
                }
                return RequestType.NO_ENDPOINT;
            default:
                return RequestType.NO_ENDPOINT;
        }
    }

    private Movie parseJsonBodyToMove(String jsonBody) {
        if (!jsonBody.isBlank()) {
            try {
                JsonElement parsedString = JsonParser.parseString(jsonBody);
                Map<String, JsonElement> parsedMap = parsedString.getAsJsonObject().asMap();

                String title = null;
                if (parsedMap.containsKey("title") && !parsedMap.get("title").getAsString().isBlank()
                        && parsedMap.get("title").getAsString().length() <= 100) {
                    title = parsedMap.get("title").getAsString();
                }

                int year = 0;
                int currentYear = LocalDate.now().getYear();
                if (parsedMap.containsKey("year")
                        && parsedMap.get("year").getAsInt() < currentYear + 1
                        && parsedMap.get("year").getAsInt() >= 1888) {
                    year = parsedMap.get("year").getAsInt();
                }
                if (title == null || year == 0) {
                    throw new IllegalArgumentException("Поля title и year отсутствуют или заполнены неправильно");
                }
                return new Movie(title, year);

            } catch (JsonSyntaxException e) {
                throw new IllegalArgumentException("Ошибка при парсинге тела запроса");
            }

        } else throw new UnsupportedOperationException("Тело запроса пустое");
    }

    private void get(HttpExchange exchange) throws IOException {
        String jsonMovieList = gson.toJson(moviesStore.getMovieList());
        sendJson(exchange, 200, jsonMovieList);
    }

    private void getId(HttpExchange exchange) throws IOException {
        int id;
        try {
            id = Integer.parseInt(exchange.getRequestURI().getPath().split("/")[2]);
            Movie movie = moviesStore.getMovieList().stream()
                    .filter(m -> m.getId() == id)
                    .findFirst()
                    .orElse(null);
            if (movie != null) {
                sendJson(exchange, 200, gson.toJson(movie));
            } else {
                ErrorResponse response = new ErrorResponse("Фильм не найден");
                sendJson(exchange, 404, gson.toJson(response));
            }

        } catch (NumberFormatException | IOException e) {
            ErrorResponse response = new ErrorResponse("Некорректный ID");
            sendJson(exchange, 400, gson.toJson(response));
        }
    }

    private void getYear(HttpExchange exchange) throws IOException {
        int year;
        try {
            year = Integer.parseInt(exchange.getRequestURI().getQuery().split("=")[1]);
            List<Movie> movieList = moviesStore.getMovieList().stream()
                    .filter(m -> m.getYear() == year)
                    .toList();
            sendJson(exchange, 200, gson.toJson(movieList));
        } catch (NumberFormatException | IOException e) {
            ErrorResponse response = new ErrorResponse("Некорректный параметр запроса — 'year'");
            sendJson(exchange, 400, gson.toJson(response));
        }
    }

    private void post(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestHeaders().containsKey("Content-Type")
                || !exchange.getRequestHeaders().get("Content-Type")
                .contains("application/json; charset=UTF-8")) {
            ErrorResponse errorResponse = new ErrorResponse("Должен быть заголовок",
                    new String[]{"Content-Type", "application/json; charset=UTF-8"});
            sendJson(exchange, 415, gson.toJson(errorResponse));
        }
        try {
            Movie movie = parseJsonBodyToMove(new String(exchange.getRequestBody().readAllBytes()));
            moviesStore.add(movie);
            sendJson(exchange, 201, gson.toJson(movie));
        } catch (UnsupportedOperationException | IllegalArgumentException e) {
            ErrorResponse errorResponse = new ErrorResponse(e.getMessage(),
                    new String[]{"название не должно быть пустым", "год должен быть между 1888 и 2026"});
            sendJson(exchange, 422, gson.toJson(errorResponse));
        }
    }

    private void delete(HttpExchange exchange) throws IOException {
        int id;
        try {
            id = Integer.parseInt(exchange.getRequestURI().getPath().split("/")[2]);
            Movie movie = moviesStore.getMovieList().stream()
                    .filter(m -> m.getId() == id)
                    .findFirst()
                    .orElse(null);
            if (movie != null) {
                moviesStore.deleteMovie(movie);
                sendNoContent(exchange);
            } else {
                ErrorResponse response = new ErrorResponse("Фильм не найден");
                sendJson(exchange, 404, gson.toJson(response));
            }

        } catch (NumberFormatException | IOException e) {
            ErrorResponse response = new ErrorResponse("Некорректный ID");
            sendJson(exchange, 400, gson.toJson(response));
        }
    }
}
