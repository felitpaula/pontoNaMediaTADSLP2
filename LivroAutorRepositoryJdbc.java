package br.edu.ifsp.biblioteca.repository;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@Repository
@Profile("jdbc")
public class LivroAutorRepositoryJdbc {

    private final DataSource dataSource;

    public LivroAutorRepositoryJdbc(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void excluirPorLivro(Connection conexao, Long livroId) throws SQLException {

        String sql = "DELETE FROM livro_autor WHERE livro_id = ?";

        try (PreparedStatement comando = conexao.prepareStatement(sql)) {
            comando.setLong(1, livroId);
            comando.executeUpdate();
        }
    }

    public void salvarRelacionamento(
            Connection conexao,
            Long livroId,
            Long autorId
    ) throws SQLException {

        String sql = "INSERT INTO livro_autor "
                + "(livro_id, autor_id) VALUES(?, ?)";

        try (PreparedStatement comando = conexao.prepareStatement(sql)) {
            comando.setLong(1, livroId);
            comando.setLong(2, autorId);

            comando.executeUpdate();
        }
    }
}