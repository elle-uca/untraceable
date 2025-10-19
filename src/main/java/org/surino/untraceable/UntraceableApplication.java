package org.surino.untraceable;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.surino.untraceable.view.MainFrame;

@SpringBootApplication
public class UntraceableApplication {

	public static void main(String[] args) {
        // Avvia Spring Boot ma disattiva "headless mode"
        // (serve per poter usare Swing)
        ConfigurableApplicationContext context =
            new SpringApplicationBuilder(UntraceableApplication.class)
                .headless(false)
                .web(WebApplicationType.SERVLET)  
                .run(args);

        // Mostra la finestra Swing nel thread della GUI
        javax.swing.SwingUtilities.invokeLater(() -> {
            MainFrame frame = context.getBean(MainFrame.class);
            frame.setVisible(true);
        });
	}

}
