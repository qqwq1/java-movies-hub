package ru.practicum.moviehub.model;

import com.google.gson.Gson;

import java.util.Objects;

public class Movie {
    private final String title;
    private final int id;
    private final int year;

    public Movie(String title, int year) {
        this.title = title;
        this.year = year;
        this.id = title.hashCode() + Objects.hashCode(year);
    }

    public String getTitle() {
        return title;
    }

    public int getId() {
        return id;
    }

    public int getYear() {
        return year;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Movie movie = (Movie) o;
        return year == movie.year && Objects.equals(title, movie.title);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, year);
    }

    @Override
    public String toString() {
        Gson gson = new Gson();
        return gson.toJson(this, Movie.class);
    }
}