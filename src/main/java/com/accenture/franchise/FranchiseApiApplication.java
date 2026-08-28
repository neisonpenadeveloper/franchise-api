package com.accenture.franchise;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada de la aplicacion.
 *
 * <p>Vive en la raiz del paquete para que el component scan de Spring solo
 * alcance {@code infrastructure}, que es la unica capa que conoce el framework.</p>
 */
@SpringBootApplication
public class FranchiseApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(FranchiseApiApplication.class, args);
    }
}
