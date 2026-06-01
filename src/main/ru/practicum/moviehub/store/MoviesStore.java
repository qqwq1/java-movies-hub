package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;

import java.util.ArrayList;
import java.util.List;

public class MoviesStore {
    private final List<Movie> movieList;

    public MoviesStore() {
        this.movieList = new ArrayList<>();
    }

    public void add(Movie movie) {
        movieList.add(movie);
    }

    public List<Movie> getMovieList() {
        return movieList;
    }

    public void clear() {
        movieList.clear();
    }

    public void deleteMovie(Movie movie) {
        movieList.remove(movie);
    }

    public int getSize() {
        return movieList.size();
    }
}