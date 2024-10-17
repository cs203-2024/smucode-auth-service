package com.cs203.smucode.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author: gav
 * @version: 1.0
 * @since: 2024-09-05
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String username;

    @Column(unique = true)
    private String email;
    private String password;
    private String profileImageUrl;

    @Enumerated(EnumType.STRING)
    private UserRole userRole;

    // TrueSkill attributes
    private double mu;
    private double sigma;
    private double skillIndex;
}
