package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;

import java.util.ArrayList;
import java.util.List;

public class MoviesStore {
    private final List<Movie> movieList;
    public int size;

    public MoviesStore() {
        this.movieList = new ArrayList<>();
        size = 0;
    }

    public void add(Movie movie) {
        movieList.add(movie);
        size++;
    }

    public List<Movie> getMovieList() {
        return movieList;
    }

    public void clear() {
        movieList.clear();
        size = 0;
    }

    public void deleteMovie(Movie movie) {
        if (movieList.remove(movie)) {
            size--;
        }
    }
}