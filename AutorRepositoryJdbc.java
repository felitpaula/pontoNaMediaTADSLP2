package br.edu.ifsp.biblioteca.repository;

import br.edu.ifsp.biblioteca.domain.Autor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
@Profile("jdbc")
public class AutorRepositoryJdbc implements IAutorRepository {

    private final DataSource dataSource;

    public AutorRepositoryJdbc(DataSource dataSource) {this.dataSource = dataSource; }

    @Override
    public Autor salvar(Autor autor) {

        try(Connection conexao = this.dataSource.getConnection())
        {
            conexao.setAutoCommit(false);

            if (autor.getId() == null) {
                this.inserirAutor(conexao, autor);
            } else {
                this.atualizarAutor(conexao, autor);
            }
        } catch (SQLException erro) {
            throw new IllegalStateException("Falha ao salvar o autor", erro);
        }

        return autor;
    }

    public Autor salvar(Connection conexao, Autor autor) throws SQLException {

        if (autor.getId() == null) {
            this.inserirAutor(conexao, autor);
        } else {
            this.atualizarAutor(conexao, autor);
        }

        return autor;
    }

    private void inserirAutor(Connection conexao, Autor autor) throws SQLException {
        String sql = "INSERT INTO autor(nome) VALUES(?)";

        try(
            PreparedStatement comando = conexao.prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS
            )
        ) {
            comando.setString(1, autor.getNome());
            comando.executeUpdate();

            try (ResultSet rs = comando.getGeneratedKeys()) {
                if (rs.next()) {
                    autor.setId(rs.getLong(1));
                }
            }
        }
    }

    private void atualizarAutor(Connection conexao, Autor autor) throws SQLException {

        String sql = "UPDATE autor SET nome = ? WHERE id = ?";

        try (PreparedStatement comando = conexao.prepareStatement(sql)) {

            comando.setString(1, autor.getNome());
            comando.setLong(2, autor.getId());

            comando.executeUpdate();
        }
    }

    private Optional<Autor> buscarUm(String sql, Object valorParametro) {

        try (Connection conexao = this.dataSource.getConnection()) {

            PreparedStatement comando = conexao.prepareStatement(sql);
            comando.setObject(1, valorParametro);

            try (ResultSet resultado = comando.executeQuery()) {
                if(resultado.next()) {

                    Autor autor = this.mapear(resultado);

                    return Optional.of(autor);
                }

                return Optional.empty();
            }
        } catch (SQLException erro) {
            throw new IllegalStateException("Falha ao consultar autor", erro);
        }
    }

    private Autor mapear(ResultSet resultado) throws SQLException {

        Autor autor = new Autor(
                resultado.getString("nome")
        );

        autor.setId(resultado.getLong("id"));

        return autor;
    }

    @Override
    public List<Autor> listarTodos() {
       String sql = "SELECT * FROM autor ORDER BY nome";

       try (Connection conexao = this.dataSource.getConnection()) {

           PreparedStatement comando = conexao.prepareStatement(sql);

           return this.listar(conexao, comando);
       } catch (SQLException erro) {
           throw new IllegalStateException("Falha ao consultar autores", erro);
       }
    }

    @Override
    public Optional<Autor> buscarPorId(Long id) { return this.buscarUm("SELECT * FROM autor WHERE id = ?", id); }

    @Override
    public List<Autor> buscarPorNome(String nome) {

        String sql = "SELECT * FROM autor WHERE LOWER(nome) LIKE ? ORDER BY nome";

        try (Connection conexao = this.dataSource.getConnection()) {

            PreparedStatement comando = conexao.prepareStatement(sql);
            comando.setString(1, "%" + nome + "%");

            return this.listar(conexao, comando);
        } catch (SQLException erro) {
            throw new IllegalStateException("Falha ao consultar autores", erro);
        }
    }

    private List<Autor> listar(Connection conexao, PreparedStatement comando) throws SQLException {

        List<Autor> autores = new ArrayList<>();

        try (ResultSet resultado = comando.executeQuery()) {
            while (resultado.next()) {

                Autor a = this.mapear(resultado);

                autores.add(a);
            }
        }

        return autores;
    }

    public List<Autor> buscarPorLivro(Connection conexao, Long livroId) throws SQLException {

        String sql = "SELECT a.* FROM autor a "
                + "JOIN livro_autor la ON la.autor_id = a.id "
                + "WHERE la.livro_id = ?";

        try (PreparedStatement comando = conexao.prepareStatement(sql)) {

            comando.setLong(1, livroId);

            return this.listar(conexao, comando);
        }
    }
}
