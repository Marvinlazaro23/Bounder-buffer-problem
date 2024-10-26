package proyectosisopera;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedList;

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

    public Buffer(int capacidad) {
        this.capacidad = capacidad;
    }

    public synchronized void colocarPizza(Pizza pizza) throws InterruptedException {
        while (pizzas.size() >= capacidad) {
            wait(); // Espera si el buffer está lleno
        }
        pizzas.add(pizza);
        System.out.println("Productor colocó una pizza " + pizza.getTipo() + ". Total de pizzas en el buffer: " + pizzas.size());
        notifyAll(); // Notifica a los consumidores que hay una nueva pizza en el buffer
    }

    public synchronized Pizza obtenerPizza() throws InterruptedException {
        while (pizzas.isEmpty()) {
            wait(); // Espera si el buffer está vacío
        }
        Pizza pizza = pizzas.poll();
        System.out.println("Consumidor obtuvo una pizza " + pizza.getTipo() + ". Total de pizzas en el buffer: " + pizzas.size());
        notifyAll(); // Notifica a los productores que hay espacio en el buffer
        return pizza;
    }

    public synchronized int getCantidadPizzas() {
        return pizzas.size();
    }
}

// Clase que representa el productor de pizzas
class Productor implements Runnable {
    private Buffer buffer;
    private String[] tiposDePizza = {"Pepperoni", "Margarita", "Jamón y Queso", "Jamón y Hongos", "Suprema"};
    private JTextArea textAreaProduccion;
    private int limitePizzas;

    public Productor(Buffer buffer, JTextArea textAreaProduccion, int limitePizzas) {
        this.buffer = buffer;
        this.textAreaProduccion = textAreaProduccion;
        this.limitePizzas = limitePizzas;
    }

    public void run() {
        try {
            for (int i = 1; i <= limitePizzas; i++) {
                String tipoPizza = tiposDePizza[(int) (Math.random() * tiposDePizza.length)];
                Pizza pizza = new Pizza(tipoPizza);
                buffer.colocarPizza(pizza);
                actualizarProduccion(tipoPizza, i);
                Thread.sleep(1000); // Simula la producción de una pizza
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void actualizarProduccion(String tipoPizza, int numero) {
        SwingUtilities.invokeLater(() -> {
            textAreaProduccion.append("Produciendo " + tipoPizza + " " + numero + "\n");
        });
    }
}

// Clase que representa el consumidor de pizzas
class Consumidor implements Runnable {
    private Buffer buffer;
    private JTextArea textAreaEstado;

    public Consumidor(Buffer buffer, JTextArea textAreaEstado) {
        this.buffer = buffer;
        this.textAreaEstado = textAreaEstado;
    }

    public void run() {
        try {
            while (true) {
                Pizza pizza = buffer.obtenerPizza();
                actualizarEstado(pizza);
                Thread.sleep(4000); // Simula el consumo de una pizza
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void actualizarEstado(Pizza pizza) {
        SwingUtilities.invokeLater(() -> {
            textAreaEstado.append("Consumidor está consumiendo una pizza de tipo: " + pizza.getTipo() + "\n");
        });
    }
}

public class ProyectoSisOpera {
    private static Buffer buffer;
    private static JFrame frameParametros;
    private static JFrame frameProduccion;
    private static JFrame frameEstado;
    private static JTextArea textAreaProduccion;
    private static JTextArea textAreaEstado;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            crearFrameParametros();
        });
    }

    private static void crearFrameParametros() {
        frameParametros = new JFrame("Parámetros");
        frameParametros.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frameParametros.setSize(300, 200);
        frameParametros.setLayout(new GridLayout(3, 2));

        JLabel labelConsumidores = new JLabel("Cantidad de consumidores:");
        JTextField textFieldConsumidores = new JTextField();
        JLabel labelCapacidad = new JLabel("Límite de pizzas:");
        JTextField textFieldCapacidad = new JTextField();
        JButton buttonIniciar = new JButton("Iniciar");

        buttonIniciar.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int cantidadConsumidores = Integer.parseInt(textFieldConsumidores.getText());
                int capacidad = Integer.parseInt(textFieldCapacidad.getText());
                buffer = new Buffer(capacidad);

                crearFrameProduccion(capacidad);
                crearFrameEstado();

                for (int i = 0; i < cantidadConsumidores; i++) {
                    new Thread(new Consumidor(buffer, textAreaEstado)).start();
                }
                new Thread(new Productor(buffer, textAreaProduccion, capacidad)).start();

                frameParametros.dispose();
            }
        });

        frameParametros.add(labelConsumidores);
        frameParametros.add(textFieldConsumidores);
        frameParametros.add(labelCapacidad);
        frameParametros.add(textFieldCapacidad);
        frameParametros.add(buttonIniciar);

        frameParametros.setVisible(true);
    }

    private static void crearFrameProduccion(int limitePizzas) {
        frameProduccion = new JFrame("Producción de Pizzas");
        frameProduccion.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frameProduccion.setSize(400, 300);
        frameProduccion.setLayout(new BorderLayout());

        textAreaProduccion = new JTextArea();
        textAreaProduccion.setEditable(false);
        frameProduccion.add(new JScrollPane(textAreaProduccion), BorderLayout.CENTER);

        frameProduccion.setVisible(true);
    }

    private static void crearFrameEstado() {
        frameEstado = new JFrame("Estado del Sistema");
        frameEstado.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frameEstado.setSize(400, 300);
        frameEstado.setLayout(new BorderLayout());

        textAreaEstado = new JTextArea();
        textAreaEstado.setEditable(false);
        frameEstado.add(new JScrollPane(textAreaEstado), BorderLayout.CENTER);

        Timer timer = new Timer(5000, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                actualizarEstado();
            }
        });
        timer.start();

        frameEstado.setVisible(true);
    }

    private static void actualizarEstado() {
        StringBuilder estado = new StringBuilder();
        estado.append("Total de pizzas en el buffer: ").append(buffer.getCantidadPizzas()).append("\n");
        textAreaEstado.setText(estado.toString());
    }
}



