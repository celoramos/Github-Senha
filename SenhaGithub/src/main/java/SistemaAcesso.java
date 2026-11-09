import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SistemaAcesso {
    private int tentativasMaximas = 3;
    Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        SistemaAcesso sistema = new SistemaAcesso();
        boolean acessoLiberado = sistema.autenticar();
        if (acessoLiberado) {
            sistema.consultarGitHub();
        } else {
            System.out.println("Número máximo de tentativas excedido. Encerrando o sistema.");
        }
        sistema.scanner.close();
    }

    private boolean autenticar() {
        for (int tentativa = 1; tentativa <= tentativasMaximas; tentativa++) {
            System.out.println("Digite sua senha para entrar no Github: ");
            String senha = scanner.nextLine();
            try {
                validarSenha(senha);
                System.out.println("Senha válida. Acesso permitido.\n");
                return true;
            } catch (SenhaInvalidaException e) {
                int restantes = tentativasMaximas - tentativa;
                System.out.println("Erro: " + e.getMessage());
                if (restantes > 0) {
                    System.out.println("Tentativas restantes: " + restantes + "\n");
                }
            }
        }
        return false;
    }

    private void validarSenha(String senha) {
        if (senha == null || senha.length() < 8) {
            throw new SenhaInvalidaException("A senha deve ter pelo menos 8 caracteres.");
        }
    }

    private void consultarGitHub() {
        System.out.println("Digite o nome de usuário do GitHub para consultar informações: ");
        String username = scanner.nextLine();
        String endereco = "https://api.github.com/users/" + username;
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endereco))
                    .header("Accept", "application/vnd.github.v3+json")
                    .build();
            HttpResponse<String> response = client
                    .send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 404) {
                throw new ErroConsultaGitHubException(
                        "Usuário \"" + username + "\" não encontrado no GitHub.");
            } if (response.statusCode() != 200) {
                throw new ErroConsultaGitHubException(
                        "Não foi possível consultar o GitHub agora (código HTTP "
                                + response.statusCode() + ").");
            }
            exibirInformacoes(response.body());

        } catch (IOException | InterruptedException e) {
            System.out.println("Houve um erro durante a consulta à API do GitHub.");
            e.printStackTrace();
        } catch (ErroConsultaGitHubException e) {
            System.out.println("Erro: " + e.getMessage());
        }
    }

    private static void exibirInformacoes(String json) {
        String login = extrairCampoTexto(json, "login");
        String nome = extrairCampoTexto(json, "name");
        String bio = extrairCampoTexto(json, "bio");
        String localizacao = extrairCampoTexto(json, "location");
        String repositorios = extrairCampoNumero(json, "public_repos");
        String seguidores = extrairCampoNumero(json, "followers");
        String seguindo = extrairCampoNumero(json, "following");
        String urlPerfil = extrairCampoTexto(json, "html_url");

        System.out.println("\n----- Informações do usuário -----");
        System.out.println("Login: " + valorOu(login, "não informado"));
        System.out.println("Nome: " + valorOu(nome, "não informado"));
        System.out.println("Bio: " + valorOu(bio, "não informado"));
        System.out.println("Localização: " + valorOu(localizacao, "não informado"));
        System.out.println("Repositórios públicos: " + valorOu(repositorios, "0"));
        System.out.println("Seguidores: " + valorOu(seguidores, "0"));
        System.out.println("Seguindo: " + valorOu(seguindo, "0"));
        System.out.println("Perfil: " + valorOu(urlPerfil, "não informado"));
        System.out.println("-----------------------------------\n");
    }

    private static String extrairCampoTexto(String json, String campo) {
        Pattern pattern = Pattern.compile("\"" + campo + "\"\\s*:\\s*\"([^\"]*)\"");
        Matcher matcher = pattern.matcher(json);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static String extrairCampoNumero(String json, String campo) {
        Pattern pattern = Pattern.compile("\"" + campo + "\"\\s*:\\s*(\\d+)");
        Matcher matcher = pattern.matcher(json);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static String valorOu(String valor, String padrao) {
        return (valor == null || valor.isBlank()) ? padrao : valor;
    }
}
