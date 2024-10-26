package proyectosisopera;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

// Clase que representa una pizza con un tipo específico
class Pizza {
    private String tipo;

    public Pizza(String tipo) {
        this.tipo = tipo;
    }

    public String getTipo() {
        return tipo;
    }
}

// Clase buffer compartido donde se almacenan las pizzas
class Buffer {
    private LinkedList<Pizza> pizzas = new LinkedList<>();
    private int capacidad;

    private JLabel totalPizzasLabel;
    private HashMap<String, JLabel> tipoPizzaLabels;
    private JTextArea consola;

    public Buffer(int capacidad, JLabel totalPizzasLabel, HashMap<String, JLabel> tipoPizzaLabels, JTextArea consola) {
        this.capacidad = capacidad;
        this.totalPizzasLabel = totalPizzasLabel;
        this.tipoPizzaLabels = tipoPizzaLabels;
        this.consola = consola;
    }

    public synchronized void colocarPizza(Pizza pizza) throws InterruptedException {
        while (pizzas.size() >= capacidad) {
            //totalPizzasLabel.setText("Consumidor detenido");
            System.out.println("------ Cheff detenido -----");
            wait(); // Espera si el buffer está lleno
        }
        pizzas.add(pizza);
        actualizarConteo();
        System.out.println("++++++ Cheff activo ++++++");
        imprimirEnConsola("Cheff colocó una pizza: " + pizza.getTipo());
        notify();
    }

    public synchronized Pizza obtenerPizza() throws InterruptedException {
        while (pizzas.isEmpty()) {
            //totalPizzasLabel.setText("Consumidor detenido");
            System.out.println("------ Consumidor detenido ------");
            wait(); // Espera si el buffer está vacío
        }
        Pizza pizza = pizzas.poll();
        actualizarConteo();
        System.out.println("++++++ Consumidor activo ++++++");
        imprimirEnConsola("Consumidor obtuvo una pizza: " + pizza.getTipo());
        notify();
        return pizza;
    }

    private void actualizarConteo() {
        SwingUtilities.invokeLater(() -> {
            totalPizzasLabel.setText("Total de pizzas en buffer: " + pizzas.size());
            tipoPizzaLabels.forEach((tipo, label) -> label.setText(tipo + ": 0"));

            HashMap<String, Integer> conteoTipos = new HashMap<>();
            for (Pizza pizza : pizzas) {
                conteoTipos.put(pizza.getTipo(), conteoTipos.getOrDefault(pizza.getTipo(), 0) + 1);
            }
            conteoTipos.forEach((tipo, count) -> tipoPizzaLabels.get(tipo).setText(tipo + ": " + count));
        });
    }

    private void imprimirEnConsola(String mensaje) {
        SwingUtilities.invokeLater(() -> {
            consola.append(mensaje + "\n");
            consola.setCaretPosition(consola.getDocument().getLength()); // Desplazar al final
        });
    }
}

class Productor implements Runnable {
    private Buffer buffer;
    private JLabel pizzasCocinadasLabel;
    private int pizzasCocinadas = 0;

    private String[] tiposDePizza = {"Pepperoni", "Margarita", "Jamón y Queso", "Jamón y Hongos", "Suprema"};

    public Productor(Buffer buffer, JLabel pizzasCocinadasLabel) {
        this.buffer = buffer;
        this.pizzasCocinadasLabel = pizzasCocinadasLabel;
    }

    public void run() {
        try {
            while (true) {
                String tipoPizza = tiposDePizza[(int) (Math.random() * tiposDePizza.length)];
                Pizza pizza = new Pizza(tipoPizza);
                buffer.colocarPizza(pizza);
                pizzasCocinadas++;
                SwingUtilities.invokeLater(() -> pizzasCocinadasLabel.setText(""));
                int numero = ThreadLocalRandom.current().nextInt(1500, 3001);
                Thread.sleep(numero);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

class Consumidor implements Runnable {
    private Buffer buffer;
    private JLabel pizzasConsumidasLabel;
    private int pizzasConsumidas = 0;

    public Consumidor(Buffer buffer, JLabel pizzasConsumidasLabel) {
        this.buffer = buffer;
        this.pizzasConsumidasLabel = pizzasConsumidasLabel;
    }

    public void run() {
        try {
            while (true) {
                Pizza pizza = buffer.obtenerPizza();
                pizzasConsumidas++;
                SwingUtilities.invokeLater(() -> pizzasConsumidasLabel.setText(""));
                int numero = ThreadLocalRandom.current().nextInt(1500, 4001);
                Thread.sleep(numero);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

public class ProyectoSisOpera {

    public static void main(String[] args) {
        JFrame frame = new JFrame("Productor - Consumidor");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 500);
        frame.setLayout(new BorderLayout());

        JPanel panelConteo = new JPanel(new GridLayout(3, 2));
        JLabel totalPizzasLabel = new JLabel("Total de pizzas en buffer: 0");
        JLabel pizzasCocinadasLabel = new JLabel("Pizzas cocinadas: 0");
        JLabel pizzasConsumidasLabel = new JLabel("Pizzas consumidas: 0");

        HashMap<String, JLabel> tipoPizzaLabels = new HashMap<>();
        String[] tiposDePizza = {"Pepperoni", "Margarita", "Jamón y Queso", "Jamón y Hongos", "Suprema"};
        for (String tipo : tiposDePizza) {
            JLabel label = new JLabel(tipo + ": 0");
            tipoPizzaLabels.put(tipo, label);
            panelConteo.add(label);
        }

        panelConteo.add(totalPizzasLabel);
        panelConteo.add(pizzasCocinadasLabel);
        panelConteo.add(pizzasConsumidasLabel);

        JTextArea consola = new JTextArea();
        consola.setEditable(false);
        JScrollPane scrollConsola = new JScrollPane(consola);
        scrollConsola.setPreferredSize(new Dimension(600, 150)); // Limitar el tamaño de la consola

        frame.add(panelConteo, BorderLayout.CENTER);
        frame.add(scrollConsola, BorderLayout.SOUTH);

        Buffer buffer = new Buffer(5, totalPizzasLabel, tipoPizzaLabels, consola);
        Thread productorThread = new Thread(new Productor(buffer, pizzasCocinadasLabel));
        Thread consumidorThread = new Thread(new Consumidor(buffer, pizzasConsumidasLabel));

        productorThread.start();
        consumidorThread.start();

        frame.setVisible(true);
    }
}
