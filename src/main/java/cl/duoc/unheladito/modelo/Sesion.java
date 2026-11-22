package cl.duoc.unheladito.modelo;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "sesion")
public class Sesion {
    @Id @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(nullable = false)
    private Instant vence;

    protected Sesion() {}

    public Sesion(String id, Usuario usuario, Instant vence) {
        this.id = id;
        this.usuario = usuario;
        this.vence = vence;
    }

    public String getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public Instant getVence() { return vence; }
}
