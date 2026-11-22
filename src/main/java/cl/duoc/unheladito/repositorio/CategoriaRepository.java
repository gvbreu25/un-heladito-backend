package cl.duoc.unheladito.repositorio;

import cl.duoc.unheladito.modelo.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
    List<Categoria> findAllByOrderByIdAsc();
}
