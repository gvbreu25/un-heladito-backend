package cl.duoc.unheladito.dto;

import cl.duoc.unheladito.modelo.Usuario;
import cl.duoc.unheladito.modelo.Rol;

public record UsuarioVista(Long id, String name, String email, Rol role) {
    public static UsuarioVista de(Usuario usuario) {
        return new UsuarioVista(usuario.getId(), usuario.getNombre(), usuario.getCorreo(), usuario.getRol());
    }
}
