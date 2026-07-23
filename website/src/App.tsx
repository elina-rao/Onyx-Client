import { Routes, Route } from "react-router-dom";
import { SiteShell } from "./components/SiteShell";
import { Home } from "./pages/Home";
import { Client } from "./pages/Client";
import { Rules } from "./pages/Rules";
import { Leaderboard } from "./pages/Leaderboard";
import { Placeholder } from "./pages/Placeholder";

export default function App() {
  return (
    <Routes>
      <Route element={<SiteShell />}>
        <Route index element={<Home />} />
        <Route path="/client" element={<Client />} />
        <Route path="/leaderboard" element={<Leaderboard />} />
        <Route path="/rules" element={<Rules />} />
        <Route
          path="/store"
          element={
            <Placeholder
              title="Store"
              blurb="Support Onyx and grab ranks and perks. The store opens soon."
            />
          }
        />
        <Route
          path="/terms"
          element={
            <Placeholder
              title="Terms of Service"
              blurb="Our Terms covering the launcher, client, server, and site will be published here."
            />
          }
        />
        <Route
          path="/privacy"
          element={
            <Placeholder
              title="Privacy Policy"
              blurb="How we handle your data — coming with the legal pass."
            />
          }
        />
        <Route
          path="*"
          element={
            <Placeholder
              title="404"
              blurb="That page doesn't exist. Head back home or hop in the Discord."
            />
          }
        />
      </Route>
    </Routes>
  );
}
