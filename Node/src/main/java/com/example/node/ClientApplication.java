package com.example.node;

import com.example.node.CLI.CLIDaemon;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class ClientApplication
{
    private static ConfigurableApplicationContext context;

    public static void main(String[] args)
    {
        context = SpringApplication.run(ClientApplication.class, args);
        context.getBean(CLIDaemon.class).run();
    }

    public static void exitApplication()
    {
        int staticExitCode = SpringApplication.exit(context, new ExitCodeGenerator()
        {
            @Override
            public int getExitCode() {
                // no errors
                return 0;
            }
        });

        System.exit(staticExitCode );
    }
}
