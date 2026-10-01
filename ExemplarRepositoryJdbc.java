package br.edu.ifsp.biblioteca.repository;

import br.edu.ifsp.biblioteca.domain.EStatusExemplar;
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
public class ExemplarRepositoryJdbc implements IExemplarRepository {

    private final DataSource dataSource;

    public ExemplarRepositoryJdbc(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Exemplar salvar(Exemplar exemplar) {

        try (Connection conexao = this.dataSource.getConnection()) {
            conexao.setAutoCommit(false);

            if (exemplar.getId() == null) {
                this.inserirExemplar(conexao, exemplar);
            } else {
                this.atualizarExemplar(conexao, exemplar);
            }
        } catch (SQLException erro) {
            throw new IllegalStateException("Falha ao salvar o exemplar", erro);
        }

        return exemplar;
    }

    public Exemplar salvar(Connection conexao, Exemplar exemplar) throws SQLException {

        if (exemplar.getId() == null) {
            this.inserirExemplar(conexao, exemplar);
        } else {
            this.atualizarExemplar(conexao, exemplar);
        }

        return exemplar;
    }

    private void inserirExemplar(Connection conexao, Exemplar exemplar) throws SQLException {

        String sql = "INSERT INTO exemplar(codigo, status, livro_id) VALUES(?, ?, ?)";

        try (
                PreparedStatement comando = conexao.prepareStatement(
                        sql, Statement.RETURN_GENERATED_KEYS
                )
        ) {
            comando.setString(1, exemplar.getCodigo());
            comando.setString(2, exemplar.getStatus().name());
            comando.setLong(3, exemplar.getLivro().getId());

            comando.executeUpdate();

            try (ResultSet rs = comando.getGeneratedKeys()) {
                if (rs.next()) {
                    exemplar.setId(rs.getLong(1));
                }
            }
        }
    }

    private void atualizarExemplar(Connection conexao, Exemplar exemplar) throws SQLException {

        String sql = "UPDATE exemplar SET status = ? WHERE id = ?";

        try (PreparedStatement comando = conexao.prepareStatement(sql)) {

            comando.setString(1, exemplar.getStatus().name());
            comando.setLong(2, exemplar.getId());

            comando.executeUpdate();
        }
    }

    private Optional<Exemplar> buscarUm(String sql, Object valorParametro) {

        try (Connection conexao = this.dataSource.getConnection()) {

            PreparedStatement comando = conexao.prepareStatement(sql);
            comando.setObject(1, valorParametro);

            try (ResultSet resultado = comando.executeQuery()) {
                if (resultado.next()) {

                    Exemplar exemplar = this.mapear(resultado);

                    return Optional.of(exemplar);
                }

                return Optional.empty();
            }
        } catch (SQLException erro) {
            throw new IllegalStateException("Falha ao consultar exemplar", erro);
        }
    }

    private Exemplar mapear(ResultSet resultado) throws SQLException {

        Livro livro = new Livro(
                resultado.getString("isbn"),
                resultado.getString("titulo"),
                resultado.getInt("ano_publicacao")
        );

        livro.setId(resultado.getLong("livro_id"));

        Exemplar exemplar = new Exemplar(
                resultado.getString("codigo"),
                livro
        );

        exemplar.setId(resultado.getLong("exemplar_id"));

        if (EStatusExemplar.valueOf(resultado.getString("status"))
                == EStatusExemplar.EMPRESTADO) {
            exemplar.emprestar();
        }

        return exemplar;
    }

    private Exemplar mapear(ResultSet resultado, Livro livro) throws SQLException {

        Exemplar exemplar = new Exemplar(
                resultado.getString("codigo"),
                livro
        );

        exemplar.setId(resultado.getLong("id"));

        if (EStatusExemplar.valueOf(resultado.getString("status"))
                == EStatusExemplar.EMPRESTADO) {
            exemplar.emprestar();
        }

        return exemplar;
    }

    @Override
    public List<Exemplar> listarTodos() {

        String sql = "SELECT e.id AS exemplar_id, e.codigo, e.status, "
                + "l.id AS livro_id, l.isbn, l.titulo, l.ano_publicacao "
                + "FROM exemplar e "
                + "JOIN livro l ON l.id = e.livro_id "
                + "ORDER BY e.codigo";

        try (Connection conexao = this.dataSource.getConnection()) {

            PreparedStatement comando = conexao.prepareStatement(sql);

            return this.listar(conexao, comando);
        } catch (SQLException erro) {
            throw new IllegalStateException("Falha ao consultar exemplares", erro);
        }
    }

    @Override
    public Optional<Exemplar> buscarPorId(Long id) {

        String sql = "SELECT e.id AS exemplar_id, e.codigo, e.status, "
                + "l.id AS livro_id, l.isbn, l.titulo, l.ano_publicacao "
                + "FROM exemplar e "
                + "JOIN livro l ON l.id = e.livro_id "
                + "WHERE e.id = ?";

        return this.buscarUm(sql, id);
    }

    @Override
    public Optional<Exemplar> buscarPorCodigo(String codigo) {

        String sql = "SELECT e.id AS exemplar_id, e.codigo, e.status, "
                + "l.id AS livro_id, l.isbn, l.titulo, l.ano_publicacao "
                + "FROM exemplar e "
                + "JOIN livro l ON l.id = e.livro_id "
                + "WHERE e.codigo = ?";

        return this.buscarUm(sql, codigo);
    }

    @Override
    public List<Exemplar> buscarPorStatus(EStatusExemplar status) {

        String sql = "SELECT e.id AS exemplar_id, e.codigo, e.status, "
                + "l.id AS livro_id, l.isbn, l.titulo, l.ano_publicacao "
                + "FROM exemplar e "
                + "JOIN livro l ON l.id = e.livro_id "
                + "WHERE e.status = ? "
                + "ORDER BY e.codigo";

        try (Connection conexao = this.dataSource.getConnection()) {

            PreparedStatement comando = conexao.prepareStatement(sql);
            comando.setString(1, status.name());

            return this.listar(conexao, comando);
        } catch (SQLException erro) {
            throw new IllegalStateException("Falha ao consultar exemplares", erro);
        }
    }

    @Override
    public List<Exemplar> buscarPorLivro(Livro livro) {

        String sql = "SELECT * FROM exemplar WHERE livro_id = ? ORDER BY codigo";

        try (Connection conexao = this.dataSource.getConnection()) {

            PreparedStatement comando = conexao.prepareStatement(sql);
            comando.setLong(1, livro.getId());

            return this.listar(conexao, comando, livro);
        } catch (SQLException erro) {
            throw new IllegalStateException("Falha ao consultar exemplares", erro);
        }
    }

    public List<Exemplar> buscarPorLivro(
            Connection conexao,
            Livro livro
    ) throws SQLException {

        String sql = "SELECT * FROM exemplar WHERE livro_id = ? ORDER BY codigo";

        try (PreparedStatement comando = conexao.prepareStatement(sql)) {

            comando.setLong(1, livro.getId());

            return this.listar(conexao, comando, livro);
        }
    }

    private List<Exemplar> listar(
            Connection conexao,
            PreparedStatement comando
    ) throws SQLException {

        List<Exemplar> exemplares = new ArrayList<>();

        try (ResultSet resultado = comando.executeQuery()) {
            while (resultado.next()) {

                Exemplar exemplar = this.mapear(resultado);

                exemplares.add(exemplar);
            }
        }

        return exemplares;
    }

    private List<Exemplar> listar(
            Connection conexao,
            PreparedStatement comando,
            Livro livro
    ) throws SQLException {

        List<Exemplar> exemplares = new ArrayList<>();

        try (ResultSet resultado = comando.executeQuery()) {
            while (resultado.next()) {

                Exemplar exemplar = this.mapear(resultado, livro);

                exemplares.add(exemplar);
            }
        }

        return exemplares;
    }
}