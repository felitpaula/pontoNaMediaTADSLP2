package br.edu.ifsp.biblioteca.repository;

import br.edu.ifsp.biblioteca.domain.Autor;
import br.edu.ifsp.biblioteca.domain.Exemplar;
import br.edu.ifsp.biblioteca.domain.Livro;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
@Profile("jdbc")
public class LivroRepositoryJdbc implements ILivroRepository {

    private final DataSource dataSource;
    private final AutorRepositoryJdbc autorRepository;
    private final ExemplarRepositoryJdbc exemplarRepository;
    private final LivroAutorRepositoryJdbc livroAutorRepository;

    public LivroRepositoryJdbc(
            DataSource dataSource,
            AutorRepositoryJdbc autorRepository,
            ExemplarRepositoryJdbc exemplarRepository,
            LivroAutorRepositoryJdbc livroAutorRepository
    ) {
        this.dataSource = dataSource;
        this.autorRepository = autorRepository;
        this.exemplarRepository = exemplarRepository;
        this.livroAutorRepository = livroAutorRepository;
    }

    @Override
    public Livro salvar(Livro livro) {

        try(Connection conexao = this.dataSource.getConnection())
        {
            conexao.setAutoCommit(false);

            if (livro.getId() == null) {
                this.inserirLivro(conexao, livro);
            } else {
                this.atualizarLivro(conexao, livro);
            }

            this.salvarAutores(conexao, livro);
            this.salvarExemplares(conexao, livro);

        } catch (SQLException erro) {
            throw new IllegalStateException("Falha ao salvar o livro", erro);
        }

        return livro;
    }

    private void inserirLivro(Connection conexao, Livro livro) throws SQLException {

        String sql = "INSERT INTO livro(isbn, titulo, ano_publicacao) VALUES (?, ?, ?)";

        try (
            PreparedStatement comando = conexao.prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS
            )
        ) {

            comando.setString(1, livro.getIsbn());
            comando.setString(2, livro.getTitulo());
            comando.setObject(3, livro.getAnoPublicacao());
            comando.executeUpdate();

            try (ResultSet rs = comando.getGeneratedKeys()) {
                if (rs.next()) {
                    livro.setId(rs.getLong(1));
                }
            }
        }
    }

    private void atualizarLivro(Connection conexao, Livro livro) throws SQLException {

        String sql = "UPDATE livro SET isbn = ?, titulo = ?, ano_publicacao = ? WHERE id = ?";


        try (PreparedStatement  comando = conexao.prepareStatement(sql)) {

            comando.setString(1, livro.getIsbn());
            comando.setString(2, livro.getTitulo());
            comando.setObject(3, livro.getAnoPublicacao());
            comando.setLong(4, livro.getId());

            comando.executeUpdate();
        }
    }

    private void salvarAutores(Connection conexao, Livro livro) throws SQLException {

        livroAutorRepository.excluirPorLivro(
                conexao,
                livro.getId()
        );

        for (Autor autor : livro.getAutores()) {

            autorRepository.salvar(conexao, autor);

            livroAutorRepository.salvarRelacionamento(
                    conexao,
                    livro.getId(),
                    autor.getId()
            );
        }
    }

    private void salvarExemplares(Connection conexao, Livro livro) throws SQLException {

        for (Exemplar exemplar : livro.getExemplares()) {
            exemplarRepository.salvar(conexao, exemplar);
        }
    }

    private Optional<Livro> buscarUm(String sql, Object valorParametro) {

        try (Connection conexao = this.dataSource.getConnection()) {

            PreparedStatement comando = conexao.prepareStatement(sql);
            comando.setObject(1, valorParametro);

            try (ResultSet resultado = comando.executeQuery()) {
                if (resultado.next()) {

                    Livro livro = this.mapear(resultado);

                    this.carregarAutores(conexao, livro);
                    this.carregarExemplares(conexao, livro);

                    return Optional.of(livro);
                }

                return Optional.empty();
            }

        } catch (SQLException erro) {
            throw new IllegalStateException("Falha ao consultar livro", erro);
        }
    }

    private void carregarAutores(Connection conexao, Livro livro) throws SQLException {

        List<Autor> autores = autorRepository.buscarPorLivro(
                conexao, livro.getId()
        );

        for (Autor autor : autores) {
            livro.adicionarAutor(autor);
        }
    }

    private void carregarExemplares(Connection conexao, Livro livro) throws SQLException {

        List<Exemplar> exemplares = exemplarRepository.buscarPorLivro(
                conexao,
                livro
        );

        for (Exemplar exemplar : exemplares) {
            livro.adicionarExemplar(exemplar);
        }
    }

    private Livro mapear(ResultSet resultado) throws SQLException {

        Livro livro = new Livro(
            resultado.getString("isbn"),
            resultado.getString("titulo"),
            resultado.getInt("ano_publicacao")
        );

        livro.setId(resultado.getLong("id"));

        return livro;
    }

    @Override
    public List<Livro> listarTodos() {
        String sql = "SELECT * FROM livro ORDER BY titulo";

        try (Connection conexao  = this.dataSource.getConnection()) {

            PreparedStatement comando = conexao.prepareStatement(sql);

            return this.listar(conexao, comando);

        } catch (SQLException erro) {
            throw new IllegalStateException("Falha ao listar livros", erro);
        }
    }

    @Override
    public Optional<Livro> buscarPorId(Long id) {
        return this.buscarUm("SELECT * FROM livro WHERE id = ?", id);
    }

    @Override
    public Optional<Livro> buscarPorIsbn(String isbn) {
        return this.buscarUm("SELECT * FROM livro WHERE isbn = ?", isbn);
    }

    @Override
    public List<Livro> buscarPorTitulo(String titulo) {

        String sql = "SELECT * FROM livro WHERE LOWER(titulo) LIKE ? ORDER BY titulo";

        try (Connection conexao = this.dataSource.getConnection()) {

            PreparedStatement comando = conexao.prepareStatement(sql);
            comando.setString(1, "%" + titulo + "%");

            return this.listar(conexao, comando);

        } catch (SQLException erro) {
            throw new IllegalStateException("Falha ao consultar livros", erro);
        }
    }

    private List<Livro> listar(Connection conexao, PreparedStatement comando) throws SQLException {

        List<Livro> livros = new ArrayList<>();

        try (ResultSet resultado = comando.executeQuery()) {
            while (resultado.next()) {

                Livro l = this.mapear(resultado);
                this.carregarAutores(conexao, l);
                this.carregarExemplares(conexao, l);

                livros.add(l);
            }
        }

        return livros;
    }
}
