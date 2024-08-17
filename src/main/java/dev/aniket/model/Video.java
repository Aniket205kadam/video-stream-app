package dev.aniket.model;

import jakarta.persistence.*;
import lombok.*;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Entity
@Table(name = "videos")
public class Video {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer videoId;

    @Column(unique = true)
    private String title;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private String contentType;

    @Column(nullable = false, length = 500, unique = true)
    private String filePath;

    @Column(nullable = false, unique = true)
    private String uniqueId;

//    @ManyToOne
//    private Course course;
}
