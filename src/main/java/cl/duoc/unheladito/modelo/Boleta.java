package cl.duoc.unheladito.modelo;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "boleta")
public class Boleta {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(nullable = false)
    private Instant fecha = Instant.now();

    @Column(nullable = false, length = 100)
    private String nombreEntrega;

    @Column(nullable = false, length = 150)
    private String correoEntrega;

    @Column(nullable = false, length = 160)
    private String direccion;

    @Column(nullable = false, length = 60)
    private String comuna;

    @Column(nullable = false, length = 60)
    private String region;

    @Column(nullable = false, length = 50)
    private String entrega;

    @Column(length = 300)
    private String notas;

    @Column(nullable = false)
    private long total;

    @OneToMany(mappedBy = "boleta", cascade = CascadeType.ALL)
    @OrderBy("id")
    private List<DetalleBoleta> detalles = new ArrayList<>();

    protected Boleta() {}

    public Boleta(Usuario usuario, String nombreEntrega, String correoEntrega, String direccion,
                  String comuna, String region, String entrega, String notas) {
        this.usuario = usuario;
        this.nombreEntrega = nombreEntrega;
        this.correoEntrega = correoEntrega;
        this.direccion = direccion;
        this.comuna = comuna;
        this.region = region;
        this.entrega = entrega;
        this.notas = notas;
    }

    public void agregarDetalle(Producto producto, int cantidad) {
        DetalleBoleta detalle = new DetalleBoleta(this, producto, cantidad);
        detalles.add(detalle);
        total += detalle.getSubtotal();
    }

    public Long getId() { return id; }
    public Instant getFecha() { return fecha; }
    public String getNombreEntrega() { return nombreEntrega; }
    public String getCorreoEntrega() { return correoEntrega; }
    public String getDireccion() { return direccion; }
    public String getComuna() { return comuna; }
    public String getRegion() { return region; }
    public String getEntrega() { return entrega; }
    public String getNotas() { return notas; }
    public long getTotal() { return total; }
    public List<DetalleBoleta> getDetalles() { return detalles; }
}
