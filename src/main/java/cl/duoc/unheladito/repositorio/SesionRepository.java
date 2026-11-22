package cl.duoc.unheladito.repositorio;

import cl.duoc.unheladito.modelo.Sesion;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.Optional;

public interface SesionRepository extends JpaRepository<Sesion, String> {
    boolean existsByIdAndUsuarioIdAndVenceAfter(String id, Long usuarioId, Instant ahora);
    long deleteByVenceBefore(Instant ahora);

    @Override
    @EntityGraph(attributePaths = "usuario")
    Optional<Sesion> findById(String id);
}
