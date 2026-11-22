package cl.duoc.unheladito.dto;

import cl.duoc.unheladito.modelo.Producto;

public record ProductoVista(String id, String name, Long categoryId, String category, int price,
                           int stock, String format, String allergens, String description,
                           String image, boolean offer) {
    public static ProductoVista de(Producto producto) {
        return new ProductoVista(producto.getId(), producto.getNombre(), producto.getCategoria().getId(),
                producto.getCategoria().getNombre(), producto.getPrecio(), producto.getStock(),
                producto.getFormato(), producto.getAlergenos(), producto.getDescripcion(),
                producto.getImagen(), producto.isOferta());
    }
}
