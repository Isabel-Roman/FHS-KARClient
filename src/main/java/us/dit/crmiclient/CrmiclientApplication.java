package us.dit.crmiclient;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

import science.aist.msbpmn.service.configuration.TransformationConfiguration;

@SpringBootApplication
@Import(TransformationConfiguration.class)
public class CrmiclientApplication {

	public static void main(String[] args) {
		SpringApplication.run(CrmiclientApplication.class, args);
	}

}
