import MapComponent from '../components/Map/MapComponent';

/**
 * Main map component page.
 * Acts as the primary secured interface after successful authentication.
 *
 * @returns {JSX.Element}
 */
export default function MapPage() {
    return (
        <div className="w-full h-screen">
            <MapComponent />
        </div>
    );
}