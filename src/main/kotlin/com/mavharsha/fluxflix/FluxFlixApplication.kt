package com.mavharsha.fluxflix

import org.springframework.boot.ApplicationRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.SynchronousSink
import java.time.Duration
import java.util.*

@SpringBootApplication
class FluxFlixApplication {
    fun myRunner (movieRepository: MovieRepository) = ApplicationRunner {
        val movies = Flux.just("Spicer man, The Homecoming!", "The Golden age of dump")
                .flatMap { movieRepository.save(Movie(title = it)) }

        movieRepository
                .deleteAll()
                .thenMany(movies)
                .thenMany(movieRepository.findAll())
                .subscribe({ println(it) })
    }
}

@Service
class MovieService(private val movieRepository: MovieRepository) {

    fun all() = movieRepository.findAll()
    fun byId(id: String) = movieRepository.findById(id)
    fun events(id: String) = Flux.generate({ sink: SynchronousSink<MovieEvents> -> sink.next(MovieEvents(id,Date())) })
            .delayElements(Duration.ofSeconds(1L))
}

fun main(args: Array<String>) {
    SpringApplication.run(FluxFlixApplication::class.java, *args)
}

interface MovieRepository: ReactiveMongoRepository<Movie, String>


@Document
data class Movie(@Id var id: String? = null, var title: String? = null)

data class MovieEvents(var id: String? = null, var data: Date? = null)


@RestController
class MovieController (var movieService: MovieService) {

    @GetMapping("/movies")
    fun all () = movieService.all()
    @GetMapping("/movies/{id}")
    fun byId (@PathVariable id: String) = movieService.byId(id)

    @GetMapping("/movies/{id}/events", produces = arrayOf(MediaType.TEXT_EVENT_STREAM_VALUE))
    fun eventsById (@PathVariable id: String) = movieService.events(id)
}
