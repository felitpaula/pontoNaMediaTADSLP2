package br.edu.ifsp.biblioteca.repository;

import br.edu.ifsp.biblioteca.domain.Usuario;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
@Profile("jdbc")
public class UsuarioRepositoryJdbc implements IUsuarioRepository {

    private final DataSource dataSource;

    public UsuarioRepositoryJdbc(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Usuario salvar(Usuario usuario) {

        try (Connection conexao = this.dataSource.getConnection()) {
            conexao.setAutoCommit(false);

            if (usuario.getId() == null) {
                this.inserirUsuario(conexao, usuario);
            } else {
                this.atualizarUsuario(conexao, usuario);
            }
        } catch (SQLException erro) {
            throw new IllegalStateException("Falha ao salvar o usuário", erro);
        }

        return usuario;
    }

    private void inserirUsuario(Connection conexao, Usuario usuario) throws SQLException {

        String sql = "INSERT INTO usuario(nome, email) VALUES(?, ?)";

        try (
                PreparedStatement comando = conexao.prepareStatement(
                        sql, Statement.RETURN_GENERATED_KEYS
                )
        ) {
            comando.setString(1, usuario.getNome());
            comando.setString(2, usuario.getEmail());

            comando.executeUpdate();

            try (ResultSet rs = comando.getGeneratedKeys()) {
                if (rs.next()) {
                    usuario.setId(rs.getLong(1));
                }
            }
        }
    }

    private void atualizarUsuario(Connection conexao, Usuario usuario) throws SQLException {

        String sql = "UPDATE usuario SET nome = ?, email = ? WHERE id = ?";

        try (PreparedStatement comando = conexao.prepareStatement(sql)) {

            comando.setString(1, usuario.getNome());
            comando.setString(2, usuario.getEmail());
            comando.setLong(3, usuario.getId());

            comando.executeUpdate();
        }
    }

    private Optional<Usuario> buscarUm(String sql, Object valorParametro) {

        try (Connection conexao = this.dataSource.getConnection()) {

            PreparedStatement comando = conexao.prepareStatement(sql);
            comando.setObject(1, valorParametro);

            try (ResultSet resultado = comando.executeQuery()) {
                if (resultado.next()) {

                    Usuario usuario = this.mapear(resultado);

                    return Optional.of(usuario);
                }

                return Optional.empty();
            }
        } catch (SQLException erro) {
            throw new IllegalStateException("Falha ao consultar usuário", erro);
        }
    }

    private Usuario mapear(ResultSet resultado) throws SQLException {

        Usuario usuario = new Usuario(
                resultado.getString("nome"),
                resultado.getString("email")
        );

        usuario.setId(resultado.getLong("id"));

        return usuario;
    }

    @Override
    public Optional<Usuario> buscarPorId(Long id) {
        return this.buscarUm(
                "SELECT * FROM usuario WHERE id = ?",
                id
        );
    }

    @Override
    public Optional<Usuario> buscarPorEmail(String email) {
        return this.buscarUm(
                "SELECT * FROM usuario WHERE email = ?",
                email
        );
    }

    @Override
    public List<Usuario> listarTodos() {

        String sql = "SELECT * FROM usuario ORDER BY nome";

        try (Connection conexao = this.dataSource.getConnection()) {

            PreparedStatement comando = conexao.prepareStatement(sql);

            return this.listar(conexao, comando);
        } catch (SQLException erro) {
            throw new IllegalStateException("Falha ao consultar usuários", erro);
        }
    }

    private List<Usuario> listar(
            Connection conexao,
            PreparedStatement comando
    ) throws SQLException {

        List<Usuario> usuarios = new ArrayList<>();

        try (ResultSet resultado = comando.executeQuery()) {
            while (resultado.next()) {

                Usuario usuario = this.mapear(resultado);

                usuarios.add(usuario);
            }
        }

        return usuarios;
    }
}