package dev.aniket.model;

import jakarta.persistence.*;

import java.util.List;

@Entity
@Table(name = "courses")
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String title;

//    @OneToMany(mappedBy = "course")
//    private List<Video> videos;
}
