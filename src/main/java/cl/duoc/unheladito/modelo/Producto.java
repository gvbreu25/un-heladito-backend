package cl.duoc.unheladito.modelo;

import jakarta.persistence.*;

@Entity
@Table(name = "producto")
public class Producto {
    @Id @Column(length = 24)
    private String id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

    @Column(nullable = false)
    private int precio;

    @Column(nullable = false)
    private int stock;

    @Column(nullable = false, length = 40)
    private String formato;

    @Column(length = 100)
    private String alergenos;

    @Column(nullable = false, length = 500)
    private String descripcion;

    @Column(length = 500)
    private String imagen;

    @Column(nullable = false)
    private boolean oferta;

    @Column(nullable = false)
    private boolean activo = true;

    protected Producto() {}

    public Producto(String id) {
        this.id = id;
    }

    public void actualizar(String nombre, Categoria categoria, int precio, int stock, String formato,
                           String alergenos, String descripcion, String imagen, boolean oferta) {
        this.nombre = nombre;
        this.categoria = categoria;
        this.precio = precio;
        this.stock = stock;
        this.formato = formato;
        this.alergenos = alergenos;
        this.descripcion = descripcion;
        this.imagen = imagen;
        this.oferta = oferta;
    }

    public void descontarStock(int cantidad) { stock -= cantidad; }
    public void desactivar() { activo = false; }
    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public Categoria getCategoria() { return categoria; }
    public int getPrecio() { return precio; }
    public int getStock() { return stock; }
    public String getFormato() { return formato; }
    public String getAlergenos() { return alergenos; }
    public String getDescripcion() { return descripcion; }
    public String getImagen() { return imagen; }
    public boolean isOferta() { return oferta; }
}
