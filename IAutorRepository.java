package br.edu.ifsp.biblioteca.repository;

import br.edu.ifsp.biblioteca.domain.Autor;

import java.util.List;
import java.util.Optional;

public interface IAutorRepository {
    Autor salvar(Autor autor);

    List<Autor> listarTodos();

    Optional<Autor> buscarPorId(Long id);

    List<Autor> buscarPorNome(String nome);
}
