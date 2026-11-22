package cl.duoc.unheladito.dto;

import cl.duoc.unheladito.modelo.Boleta;
import java.time.Instant;
import java.util.List;

public record PedidoVista(Long id, Instant createdAt, Customer customer, List<Item> items,
                         long total, String status) {
    public static PedidoVista de(Boleta boleta) {
        Customer entrega = new Customer(boleta.getNombreEntrega(), boleta.getCorreoEntrega(),
                boleta.getDireccion(), boleta.getRegion(), boleta.getComuna(), boleta.getEntrega(), boleta.getNotas());
        List<Item> items = boleta.getDetalles().stream().map(detalle -> new Item(
                detalle.getProducto().getId(), detalle.getNombreProducto(), detalle.getCantidad(),
                detalle.getPrecioUnitario(), detalle.getSubtotal())).toList();
        return new PedidoVista(boleta.getId(), boleta.getFecha(), entrega, items, boleta.getTotal(), "Registrado");
    }

    public record Customer(String name, String email, String street, String region, String commune,
                           String delivery, String notes) {}
    public record Item(String id, String name, int qty, int price, long subtotal) {}
}
