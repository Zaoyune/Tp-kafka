package org.sid.demospringcloudstreamskafka.services;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.sid.demospringcloudstreamskafka.Entities.PageEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Date;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@Service
public class PageEventService {
    @Bean
    public Consumer<PageEvent> pageEventConsumer(){
        return (input)->{
            System.out.println("**************************");
            System.out.println(input.toString());
            System.out.println("**************************");
        };
    }
    @Bean
    public Supplier<PageEvent> pageEventSupplier(){
        return ()-> new PageEvent(
                Math.random()>0.5?"P1":"P2",
                Math.random()>0.5?"U1":"U2",
                new Date(),
                new Random().nextInt(9000));
    }
    @Bean
    public Function<PageEvent,PageEvent> pageEventFunction(){
        return (input)->{
                input.setName("Page Event");
                input.setUser("UUUUUUU");
                return input;
                };
    }
    @Bean
    public Function<KStream<String,PageEvent>,KStream<String,Long>> kStreamFunction(){
        return (input)->{
            return input
                    //select from stream (filter veux dire where) where v.getName() le nom de la page grouper par la clé en faisant un count et retourner en fin le resultat sous forme d'un stream
                    .filter((k,v)->v.getDuration()>100)//traiter que les evennements de visites de pages dont la durée dépasse 100ms
                    .map((k,v)->new KeyValue<>(v.getName(),0L))
                    .groupBy((k,v)->k,Grouped.with(Serdes.String(),Serdes.Long()))
                    .windowedBy(TimeWindows.of(Duration.ofSeconds(5)))//affiche moi les statistiques des 5 dernières secondes
                    .count(Materialized.as("page-count"))//persister le resultat un fois le calcul produit le resultat dans une table ou un store nommer page-count
                    //on peux utiliser d'autre methodes comme reduce c'est le meme principe de mysql
                    .toStream()
                    //convertir windows en String ksream
                    .map((k,v)->new KeyValue<>("=>"+k.window().startTime()+k.window().endTime()+" "+k.key(),v));
            //windowedby produit toujours un objet/resultat de type KTable
            //groupeBy  produit toujours un objet/resultat de type KStream
        };
    }
}
