package br.edu.ifsp.biblioteca.repository;

import br.edu.ifsp.biblioteca.domain.EStatusExemplar;
import br.edu.ifsp.biblioteca.domain.Exemplar;
import br.edu.ifsp.biblioteca.domain.Livro;

import java.util.List;
import java.util.Optional;

public interface IExemplarRepository {
    Exemplar salvar(Exemplar exemplar);

    List<Exemplar> listarTodos();

    Optional<Exemplar> buscarPorId(Long id);

    Optional<Exemplar> buscarPorCodigo(String codigo);

    List<Exemplar> buscarPorStatus(EStatusExemplar status);

    List<Exemplar> buscarPorLivro(Livro livro);
}
