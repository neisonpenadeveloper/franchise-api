package com.accenture.franchise.infrastructure.config;

import com.accenture.franchise.application.usecase.FranchiseUseCase;
import com.accenture.franchise.domain.port.out.FranchiseRepositoryPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cablea los casos de uso con sus puertos.
 *
 * <p>El caso de uso se declara aqui, en infraestructura, en lugar de anotarlo
 * con {@code @Service}: asi las capas de dominio y aplicacion quedan libres de
 * anotaciones de Spring y se pueden instanciar en un test unitario con un
 * simple {@code new}.</p>
 */
@Configuration
public class BeanConfiguration {

    @Bean
    public FranchiseUseCase franchiseUseCase(FranchiseRepositoryPort franchiseRepositoryPort) {
        return new FranchiseUseCase(franchiseRepositoryPort);
    }
}
