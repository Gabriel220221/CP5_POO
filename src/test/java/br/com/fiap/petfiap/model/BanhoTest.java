package br.com.fiap.petfiap.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Testes unitarios do model: sem banco, sem Spring (Aula 15).
public class BanhoTest {

    private Banho banhoDoRex() {
        return new Banho(1, "Rex", "PEQUENO", "Ana", LocalDateTime.of(2026, 10, 1, 10, 0));
    }

    @Test
    public void deveAcumular20PontosDeFidelidade() {
        // Act
        int pontos = banhoDoRex().calcularPontosFidelidade();

        // Assert
        assertEquals(20, pontos);
    }

    @Test
    public void deveDurar45Minutos() {
        // Act
        int duracao = banhoDoRex().getDuracaoMinutos();

        // Assert
        assertEquals(45, duracao);
    }

    @Test
    public void deveCobrarPrecoDoBanhoConformeOPorte() {
        // Arrange
        LocalDateTime data = LocalDateTime.of(2026, 10, 1, 10, 0);
        Banho pequeno = new Banho(1, "Rex", "PEQUENO", "Ana", data);
        Banho medio = new Banho(2, "Mimi", "MEDIO", "Bruno", data);
        Banho grande = new Banho(3, "Thor", "GRANDE", "Carla", data);

        // Act + Assert: PEQUENO R$ 60, MEDIO R$ 80, GRANDE R$ 100
        assertEquals(60.0, pequeno.calcularPreco(), 0.001);
        assertEquals(80.0, medio.calcularPreco(), 0.001);
        assertEquals(100.0, grande.calcularPreco(), 0.001);
    }
}
