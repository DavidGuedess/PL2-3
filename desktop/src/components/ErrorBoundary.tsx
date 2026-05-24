import { Component, ReactNode } from "react";

interface Props { children: ReactNode; }
interface State { error: Error | null; }

export class ErrorBoundary extends Component<Props, State> {
  state: State = { error: null };

  static getDerivedStateFromError(error: Error): State {
    return { error };
  }

  render() {
    if (this.state.error) {
      return (
        <div style={{
          padding: 40, fontFamily: "monospace", background: "#111117",
          color: "#e74c3c", minHeight: "100vh",
        }}>
          <h2 style={{ marginBottom: 16 }}>Erro de renderização</h2>
          <pre style={{ whiteSpace: "pre-wrap", fontSize: 13, color: "#e0e0f0" }}>
            {this.state.error.message}
            {"\n\n"}
            {this.state.error.stack}
          </pre>
          <button
            style={{ marginTop: 24, padding: "8px 20px", background: "#4f6ef7", color: "white", border: "none", borderRadius: 6, cursor: "pointer" }}
            onClick={() => { this.setState({ error: null }); window.location.reload(); }}
          >
            Recarregar
          </button>
        </div>
      );
    }
    return this.props.children;
  }
}
