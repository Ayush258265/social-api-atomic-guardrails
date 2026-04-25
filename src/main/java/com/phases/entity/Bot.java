package com.phases.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "bots")
public class Bot {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String name;

	private String personaDescription;

	public Bot() {
		super();
		// TODO Auto-generated constructor stub
	}

	public Bot(Long id, String name, String personaDescription) {
		super();
		this.id = id;
		this.name = name;
		this.personaDescription = personaDescription;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPersonaDescription() {
		return personaDescription;
	}

	public void setPersonaDescription(String personaDescription) {
		this.personaDescription = personaDescription;
	}

}
