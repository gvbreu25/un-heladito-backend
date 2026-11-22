package cl.duoc.unheladito.repositorio;

import cl.duoc.unheladito.modelo.Producto;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ProductoRepository extends JpaRepository<Producto, String> {
    @EntityGraph(attributePaths = "categoria")
    List<Producto> findByActivoTrueOrderByIdAsc();

    @EntityGraph(attributePaths = "categoria")
    Optional<Producto> findByIdAndActivoTrue(String id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Producto p where p.id = :id and p.activo = true")
    Optional<Producto> buscarParaActualizar(@Param("id") String id);
}
