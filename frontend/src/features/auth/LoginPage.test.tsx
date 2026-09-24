import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { AuthProvider } from "./AuthProvider";
import { LoginPage } from "./LoginPage";
import * as authApi from "./api";

vi.mock("./api");

function renderLoginPage() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={["/login"]}>
        <AuthProvider>
          <LoginPage />
        </AuthProvider>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe("LoginPage", () => {
  beforeEach(() => {
    vi.mocked(authApi.fetchCurrentUser).mockRejectedValue(new Error("não autenticado"));
  });

  it("mostra erros de validação ao enviar o formulário vazio", async () => {
    const user = userEvent.setup();
    renderLoginPage();

    await user.click(await screen.findByRole("button", { name: /entrar/i }));

    expect(await screen.findByText("Informe o usuário.")).toBeInTheDocument();
    expect(await screen.findByText("Informe a senha.")).toBeInTheDocument();
    expect(authApi.login).not.toHaveBeenCalled();
  });

  it("chama o login com usuário e senha informados", async () => {
    vi.mocked(authApi.login).mockResolvedValue({
      id: "1",
      username: "marlus",
      displayName: "Marlus",
    });
    const user = userEvent.setup();
    renderLoginPage();

    await user.type(await screen.findByLabelText("Usuário"), "marlus");
    await user.type(screen.getByLabelText("Senha"), "marlus");
    await user.click(screen.getByRole("button", { name: /entrar/i }));

    await waitFor(() =>
      expect(authApi.login).toHaveBeenCalledWith({ username: "marlus", password: "marlus" }),
    );
  });
});
