package ru.hd.jpa;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "passport_scans")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PassportScan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Lob
    @Column(columnDefinition = "BYTEA", nullable = false)
    @JdbcTypeCode(SqlTypes.BINARY)
    private byte[] scan;

    @OneToOne(mappedBy = "passportScan")
    private Client client;
}