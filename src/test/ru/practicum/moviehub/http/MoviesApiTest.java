package ru.practicum.moviehub.http;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.*;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class MoviesApiTest {
    private static final String BASE = "http://localhost:8080";
    private static MoviesServer server;
    private static HttpClient client;

    @BeforeAll
    static void beforeAll() {
        server = new MoviesServer(new MoviesStore(), 8080);
        client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
        server.start();
    }

    @BeforeEach
    void beforeEach() {
        server.getMoviesStore().clear();
    }

    @AfterAll
    static void afterAll() {
        server.stop();
    }

    @DisplayName("Get /movies возвращает пустой массив json для пустого списка фильмов")
    @Test
    void getMovies_whenEmpty_returnsEmptyArray() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();
        assertTrue(body.startsWith("[") && body.endsWith("]"),
                "Ожидается JSON-массив");
    }

    @DisplayName("Get /movies возвращает массив json для непустого списка фильмов")
    @Test
    void getMovies_whenNotEmpty_returnsArray() throws Exception {
        server.getMoviesStore().add(new Movie("Название 1", 1985));
        server.getMoviesStore().add(new Movie("Название 2", 1995));
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();
        assertTrue(body.startsWith("[") && body.endsWith("]"),
                "Ожидается JSON-массив");
        JsonElement parsedJsonString = JsonParser.parseString(body);
        JsonArray parsedJsonArray = parsedJsonString.getAsJsonArray();
        assertEquals(2, parsedJsonArray.size());
        assertEquals("Название 1", parsedJsonArray.get(0).getAsJsonObject().get("title").getAsString());
        assertEquals("1985", parsedJsonArray.get(0).getAsJsonObject().get("year").getAsString());
        assertEquals("Название 2", parsedJsonArray.get(1).getAsJsonObject().get("title").getAsString());
        assertEquals("1995", parsedJsonArray.get(1).getAsJsonObject().get("year").getAsString());
    }

    /// ///////////////////////////////////////////////////////////////////////////////////////////////////////
    private HttpResponse<String> sendPost(String body, String contentType) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Accept", "application/json; charset=UTF-8")
                .header("Content-Type", contentType)
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        return client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    @DisplayName("Post /movies добавляет фильм при корректном теле запроса")
    @Test
    void postMovies_addMovies_whenCorrectBody() throws Exception {
        Movie movie = new Movie("Снежная королева", 2000);
        String jsonBody = movie.toString();

        HttpResponse<String> response = sendPost(jsonBody, "application/json; charset=UTF-8");

        assertEquals(201, response.statusCode());
        assertTrue(server.getMoviesStore().getMovieList().contains(movie));
        assertEquals(1, server.getMoviesStore().getSize());
    }

    @DisplayName("POST /movies: возвращает ошибку при пустом title")
    @Test
    void postMovies_returnsError_whenTitleIsBlank() throws Exception {
        String body = "{\"title\":\"   \",\"year\":2000}";

        HttpResponse<String> response = sendPost(body, "application/json; charset=UTF-8");

        assertEquals(422, response.statusCode());
        assertEquals(0, server.getMoviesStore().getSize());
        assertTrue(JsonParser.parseString(response.body()).getAsJsonObject().has("error"));
    }

    @DisplayName("POST /movies: возвращает ошибку при title длиннее 100 символов")
    @Test
    void postMovies_returnsError_whenTitleTooLong() throws Exception {
        String longTitle = "a".repeat(101);
        String body = "{\"title\":\"" + longTitle + "\",\"year\":2000}";

        HttpResponse<String> response = sendPost(body, "application/json; charset=UTF-8");

        assertEquals(422, response.statusCode());
        assertEquals(0, server.getMoviesStore().getSize());
        assertTrue(JsonParser.parseString(response.body()).getAsJsonObject().has("error"));
    }

    @DisplayName("POST /movies: возвращает ошибку при неверном year")
    @Test
    void postMovies_returnsError_whenYearOutOfRange() throws Exception {
        int currentYear = LocalDate.now().getYear();

        HttpResponse<String> tooOld = sendPost(
                "{\"title\":\"Film\",\"year\":1887}",
                "application/json; charset=UTF-8"
        );
        assertEquals(422, tooOld.statusCode());

        HttpResponse<String> tooFuture = sendPost(
                "{\"title\":\"Film\",\"year\":" + (currentYear + 2) + "}",
                "application/json; charset=UTF-8"
        );
        assertEquals(422, tooFuture.statusCode());

        assertEquals(0, server.getMoviesStore().getSize());
    }

    @DisplayName("POST /movies: возвращает ошибку при неправильном Content-Type")
    @Test
    void postMovies_returnsError_whenContentTypeIsWrong() throws Exception {
        String body = "{\"title\":\"Film\",\"year\":2000}";

        HttpResponse<String> response = sendPost(body, "text/plain");

        assertEquals(415, response.statusCode());
        assertEquals(0, server.getMoviesStore().getSize());
        assertTrue(JsonParser.parseString(response.body()).getAsJsonObject().has("error"));
    }

    @DisplayName("POST /movies: возвращает ошибку при некорректном JSON")
    @Test
    void postMovies_returnsError_whenJsonIsMalformed() throws Exception {
        String malformed = "{\"title\":\"Film\",\"year\":2000";

        HttpResponse<String> response = sendPost(malformed, "application/json; charset=UTF-8");

        assertEquals(422, response.statusCode());
        assertEquals(0, server.getMoviesStore().getSize());
        assertTrue(JsonParser.parseString(response.body()).getAsJsonObject().has("error"));
    }

    /// ///////////////////////////////////////////////////////////////////////////////////////////////////////
    @DisplayName("Get /movies/{id} возвращает фильм по существующему id")
    @Test
    void getMovieById_whenExists_returnsMovie() throws Exception {
        Movie movie = new Movie("Один фильм", 1999);
        server.getMoviesStore().add(movie);
        int id = movie.getId();

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/" + id))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode());
        String contentType = resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentType);

        JsonElement parsed = JsonParser.parseString(resp.body());
        Assertions.assertTrue(parsed.isJsonObject());
        var obj = parsed.getAsJsonObject();
        assertEquals(movie.getTitle(), obj.get("title").getAsString());
        assertEquals(String.valueOf(movie.getYear()), obj.get("year").getAsString());
    }

    @DisplayName("Get /movies/{id} возвращает 404, если фильм не найден")
    @Test
    void getMovieById_whenNotFound_returns404() throws Exception {
        // Числовой id, которого точно нет в store
        int missingId = 999_999_999;

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/" + missingId))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(404, resp.statusCode());
        JsonElement parsed = JsonParser.parseString(resp.body());
        Assertions.assertTrue(parsed.isJsonObject());
        var obj = parsed.getAsJsonObject();
        assertTrue(obj.has("error") || obj.has("message"));
    }

    @DisplayName("Get /movies/{id} возвращает ошибку, если id не число")
    @Test
    void getMovieById_whenIdIsNotNumber_returnsError() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/abc"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));


        assertEquals(400, resp.statusCode());
        JsonElement parsed = JsonParser.parseString(resp.body());
        Assertions.assertTrue(parsed.isJsonObject());
        var obj = parsed.getAsJsonObject();
        assertTrue(obj.has("error") || obj.has("message"));
    }

    /// ///////////////////////////////////////////////////////////////////////////////////////////////////////
    private HttpResponse<String> sendDelete(String id) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/" + id))
                .DELETE()
                .build();

        return client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    @DisplayName("DELETE /movies/{id} удаляет фильм по существующему id")
    @Test
    void deleteMovie_whenExists_deletesAndReturnsNoContent() throws Exception {
        Movie movie = new Movie("To Be Deleted", 2000);
        server.getMoviesStore().add(movie);
        int id = movie.getId();

        HttpResponse<String> resp = sendDelete(String.valueOf(id));

        assertEquals(204, resp.statusCode());
        assertTrue(resp.body() == null || resp.body().isEmpty());

        assertEquals(0, server.getMoviesStore().getSize());
        assertFalse(server.getMoviesStore().getMovieList().contains(movie));
    }

    @DisplayName("DELETE /movies/{id} возвращает 404, если фильм не найден")
    @Test
    void deleteMovie_whenNotFound_returns404() throws Exception {
        int missingId = 999_999_999;

        HttpResponse<String> resp = sendDelete(String.valueOf(missingId));

        assertEquals(404, resp.statusCode());

        JsonElement parsed = JsonParser.parseString(resp.body());
        Assertions.assertTrue(parsed.isJsonObject());
        var obj = parsed.getAsJsonObject();
        assertTrue(obj.has("error") || obj.has("message"));

        assertEquals(0, server.getMoviesStore().getSize());
    }

    @DisplayName("DELETE /movies/{id} возвращает ошибку, если id не число")
    @Test
    void deleteMovie_whenIdIsNotNumber_returnsBadRequest() throws Exception {
        Movie movie = new Movie("To Be Deleted", 2000);
        server.getMoviesStore().add(movie);
        HttpResponse<String> resp = sendDelete("abc");

        assertEquals(400, resp.statusCode());
        JsonElement parsed = JsonParser.parseString(resp.body());
        Assertions.assertTrue(parsed.isJsonObject());
        var obj = parsed.getAsJsonObject();
        assertTrue(obj.has("error") || obj.has("message"));

        assertEquals(1, server.getMoviesStore().getSize());
    }

    /// ///////////////////////////////////////////////////////////////////////////////////////////////////////
    @DisplayName("GET /movies?year=YYYY возвращает фильмы указанного года")
    @Test
    void getMoviesByYear_returnsMovies() throws Exception {
        server.getMoviesStore().add(new Movie("Film A", 2000));
        server.getMoviesStore().add(new Movie("Film B", 1999));
        server.getMoviesStore().add(new Movie("Film C", 2000));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=2000"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode());
        String ct = resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", ct);

        JsonElement parsed = JsonParser.parseString(resp.body());
        Assertions.assertTrue(parsed.isJsonArray());
        JsonArray arr = parsed.getAsJsonArray();
        assertEquals(2, arr.size());
        // Проверяем содержимое (порядок соответствует вставке в store)
        assertEquals("Film A", arr.get(0).getAsJsonObject().get("title").getAsString());
        assertEquals("2000", arr.get(0).getAsJsonObject().get("year").getAsString());
        assertEquals("Film C", arr.get(1).getAsJsonObject().get("title").getAsString());
        assertEquals("2000", arr.get(1).getAsJsonObject().get("year").getAsString());
    }

    @DisplayName("GET /movies?year=YYYY возвращает пустой список, если фильмов с таким годом нет")
    @Test
    void getMoviesByYear_returnsEmptyListWhenNone() throws Exception {
        server.getMoviesStore().add(new Movie("X", 1990));
        server.getMoviesStore().add(new Movie("Y", 1991));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=2000"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode());
        String ct = resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", ct);

        JsonElement parsed = JsonParser.parseString(resp.body());
        Assertions.assertTrue(parsed.isJsonArray());
        JsonArray arr = parsed.getAsJsonArray();
        assertEquals(0, arr.size());
    }

    @DisplayName("GET /movies?year=... возвращает ошибку, если параметр year не число")
    @Test
    void getMoviesByYear_returnsError_whenYearNotNumber() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=abc"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));


        assertEquals(400, resp.statusCode());
        JsonElement parsed = JsonParser.parseString(resp.body());
        Assertions.assertTrue(parsed.isJsonObject());
        var obj = parsed.getAsJsonObject();
        assertTrue(obj.has("error") || obj.has("message"));
    }

}

