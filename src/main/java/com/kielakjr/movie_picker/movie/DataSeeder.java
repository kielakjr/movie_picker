package com.kielakjr.movie_picker.movie;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import java.util.List;
import com.kielakjr.movie_picker.ai.EmbeddingClient;

@Component
@Profile("!test")
@RequiredArgsConstructor
public class DataSeeder implements ApplicationListener<ApplicationReadyEvent> {

    private final MovieRepository movieRepository;
    private final EmbeddingClient embeddingClient;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (movieRepository.count() > 0) return;

        List<Movie> movies = List.of(
            Movie.builder().title("Interstellar").genre("Sci-Fi")
                .description("A team of astronauts travels through a wormhole near Saturn to find a new home for humanity as Earth faces extinction due to crop failures and dust storms.").build(),
            Movie.builder().title("The Shawshank Redemption").genre("Drama")
                .description("A banker wrongly convicted of murder forms an unlikely friendship with a fellow prisoner while maintaining hope and planning his escape over two decades.").build(),
            Movie.builder().title("The Dark Knight").genre("Action")
                .description("Batman faces the Joker, a criminal mastermind who plunges Gotham City into chaos and forces the hero to confront his own moral boundaries.").build(),
            Movie.builder().title("Inception").genre("Sci-Fi")
                .description("A skilled thief who enters people's dreams to steal secrets is given a chance to have his criminal record erased if he can plant an idea in a target's mind.").build(),
            Movie.builder().title("Pulp Fiction").genre("Crime")
                .description("The lives of two mob hitmen, a boxer, a gangster and his wife intertwine in four tales of violence and redemption in Los Angeles.").build(),
            Movie.builder().title("The Godfather").genre("Crime")
                .description("The aging patriarch of an organized crime dynasty transfers control of his empire to his reluctant son, who must navigate family loyalty and brutal power struggles.").build(),
            Movie.builder().title("Forrest Gump").genre("Drama")
                .description("A slow-witted but kind-hearted man from Alabama witnesses and unwittingly influences several defining historical events in 20th century American history.").build(),
            Movie.builder().title("The Matrix").genre("Sci-Fi")
                .description("A computer hacker discovers that reality as he knows it is a simulation created by machines, and joins a rebellion to free humanity from their control.").build(),
            Movie.builder().title("Schindler's List").genre("Drama")
                .description("A German businessman saves the lives of more than a thousand Jewish refugees during the Holocaust by employing them in his factories during World War II.").build(),
            Movie.builder().title("Parasite").genre("Thriller")
                .description("A poor Korean family schemes to become employed by a wealthy family, infiltrating their household and triggering a chain of unexpected and darkly comic events.").build(),
            Movie.builder().title("Whiplash").genre("Drama")
                .description("A young jazz drummer at a prestigious music conservatory is pushed to his limits by an abusive instructor who uses fear and intimidation to achieve perfection.").build(),
            Movie.builder().title("Mad Max: Fury Road").genre("Action")
                .description("In a post-apocalyptic wasteland a woman rebels against a tyrannical ruler in search of her homeland with the aid of a group of female prisoners and a drifter.").build(),
            Movie.builder().title("Arrival").genre("Sci-Fi")
                .description("A linguist is recruited by the military to communicate with alien lifeforms after twelve mysterious spacecraft appear around the world.").build(),
            Movie.builder().title("The Silence of the Lambs").genre("Thriller")
                .description("A young FBI cadet seeks the help of an imprisoned cannibalistic killer to catch a serial murderer who skins his victims.").build(),
            Movie.builder().title("Spirited Away").genre("Animation")
                .description("A sullen ten-year-old girl wanders into a world ruled by gods, witches and spirits, and where humans are changed into beasts, and must find a way back to the human world.").build()
        );

        List<Movie> moviesWithEmbeddings = movies.stream()
                .map(movie -> {
                    float[] embedding = embeddingClient.getEmbedding(
                            movie.getTitle() + " " + movie.getGenre() + " " + movie.getDescription()
                    );
                    movie.setEmbedding(embedding);
                    return movie;
                })
                .toList();

        movieRepository.saveAll(moviesWithEmbeddings);
    }
}
